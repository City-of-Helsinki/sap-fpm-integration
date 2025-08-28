import os
import socket
import paramiko
from paramiko import ServerInterface, SFTPServerInterface, SFTPServer, SFTPAttributes, \
    SFTPHandle, SFTP_OK, AUTH_SUCCESSFUL, OPEN_SUCCEEDED, AUTH_FAILED

from robot.api.deco import keyword, not_keyword
from threading import Thread, current_thread

# can pass in user dirs here
class S4Server (ServerInterface):
    def check_auth_password(self, username, password):
        self.logged_in_user = username
        self.logged_in_user_dir = self.user_dirs.get(username)
        return AUTH_SUCCESSFUL

    def check_auth_publickey(self, username, key):
        return AUTH_FAILED

    def check_channel_request(self, kind, chanid):
        return OPEN_SUCCEEDED

    def get_allowed_auths(self, username):
        return "password"

    def check_channel_shell_request(self, channel):
        return True



class S4SFTPHandle (SFTPHandle):
    def stat(self):
        try:
            return SFTPAttributes.from_stat(os.fstat(self.readfile.fileno()))
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

# paramiko subsystem handler
class S4SFTPServerHandler (SFTPServerInterface):

    def __init__(self, server, *args, **kwargs):
        self.server = server
        super().__init__(*args, **kwargs)

    def _realpath(self, path):
        return self.server.logged_in_user_dir + self.canonicalize(path)

    def list_folder(self, path):
        path = self._realpath(path)
        try:
            out = [ ]
            flist = os.listdir(path)
            for fname in flist:
                attr = SFTPAttributes.from_stat(os.stat(os.path.join(path, fname)))
                attr.filename = fname
                out.append(attr)
            return out
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

    def _stat(self, path, run_stat):
        try:
            return SFTPAttributes.from_stat(run_stat(self._realpath(path)))
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

    def stat(self, path):
       return self._stat(path, os.stat)

    def lstat(self, path):
        return self._stat(path, os.lstat)

    def open(self, path, flags, attr):
        path = self._realpath(path)
        try:
            binary_flag = getattr(os, 'O_BINARY',  0)
            flags |= binary_flag
            mode = getattr(attr, 'st_mode', None)
            if mode is not None:
                fd = os.open(path, flags, mode)
            else:
                # os.open() defaults to 0777 which is
                # an odd default mode for files
                fd = os.open(path, flags, 0o666)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        if (flags & os.O_CREAT) and (attr is not None):
            attr._flags &= ~attr.FLAG_PERMISSIONS
            SFTPServer.set_file_attr(path, attr)
        if flags & os.O_WRONLY:
            if flags & os.O_APPEND:
                fstr = 'ab'
            else:
                fstr = 'wb'
        elif flags & os.O_RDWR:
            if flags & os.O_APPEND:
                fstr = 'a+b'
            else:
                fstr = 'r+b'
        else:
            # O_RDONLY (== 0)
            fstr = 'rb'
        try:
            f = os.fdopen(fd, fstr)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        fobj = S4SFTPHandle(flags)
        fobj.filename = path
        fobj.readfile = f
        fobj.writefile = f
        return fobj

    def _OK_or_ERR(self, exec):
        try:
            exec()
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        return SFTP_OK

    def remove(self, path):
        return self._OK_or_ERR(lambda: os.remove(self._realpath(path)))

    def rename(self, oldpath, newpath):
        return self._OK_or_ERR(lambda: os.rename(self._realpath(oldpath), self._realpath(newpath)))

    def mkdir(self, path, attr):
        path = self._realpath(path)
        def do_exec():
            os.mkdir(path)
            if attr is not None:
                SFTPServer.set_file_attr(path, attr)
        return self._OK_or_ERR(do_exec)

    def rmdir(self, path):
        return self._OK_or_ERR(lambda: os.rmdir(self._realpath(path)))

    def chattr(self, path, attr):
        return self._OK_or_ERR(lambda: SFTPServer.set_file_attr(self._realpath(path), attr))

class S4SFTPServer(object):

    ROBOT_LIBRARY_SCOPE = 'GLOBAL'

    def __init__(self):
        self.user_dirs = {}

    @keyword(types=['string'])
    def init_sftp_server(self, relative_ftp_dir):
        self.ftp_dir = os.path.join(os.getcwd(), relative_ftp_dir)
        if not os.path.exists(self.ftp_dir): os.makedirs(self.ftp_dir)

    @keyword()
    def start_sftp_server(self):
        paramiko.common.logging.basicConfig(level=paramiko.common.logging.DEBUG)
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, True)
        host = "0.0.0.0"
        port = 2122
        server_socket.bind((host, port))
        server_socket.listen(10)

        def serve_forever(server):
            conn, addr = server_socket.accept()
            host_key = paramiko.RSAKey.from_private_key_file("/tmp/robot_id_rsa")
            transport = paramiko.Transport(conn)
            transport.add_server_key(host_key)
            transport.set_subsystem_handler('sftp', paramiko.SFTPServer, S4SFTPServerHandler)

            transport.start_server(server=server)
            self.channel = transport.accept()
            while transport.is_active():
                time.sleep(1)

        self.server = S4Server()
        self.server.ftp_dir = self.ftp_dir
        self.server.user_dirs = self.user_dirs
        self.ftp_thread = Thread(target=serve_forever, args=(self.server, ))
        self.ftp_thread.setDaemon(True)
        self.ftp_thread.start()

    @keyword(types=['string', 'string', 'string', 'list'])
    def add_sftp_user(self, user, password, dir_name, subdir_names):
        user_dir = os.path.join(self.ftp_dir, dir_name)
        if not os.path.exists(user_dir): os.makedirs(user_dir)
        for subdir_name in subdir_names:
            dir = os.path.join(self.ftp_dir, dir_name, subdir_name)
            if not os.path.exists(dir): os.makedirs(dir)
        #.server.handler.authorizer.add_user(user, password, user_dir, perm="elradfmwMT")
        self.user_dirs[user] = user_dir
        return user_dir

    @keyword(types=['string'])
    def get_sftp_dir_for(self, user):
        return self.user_dirs.get(user)

    @keyword()
    def get_main_sftp_dir(self):
        return self.ftp_dir

    @keyword()
    def close_sftp_server(self):
        self.channel.close()

    @keyword()
    def set_sftp_connection_as_down(self):
        self.connection_is_down = True

    @keyword()
    def set_sftp_connection_as_up(self):
        self.connection_is_down = False