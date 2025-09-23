import os
import socket
import paramiko
import time
from paramiko import ServerInterface, SFTPServerInterface, SFTPServer, SFTPAttributes, \
    SFTPHandle, SFTP_FAILURE, AUTH_SUCCESSFUL, OPEN_SUCCEEDED, AUTH_FAILED

from robot.api.deco import keyword, not_keyword
from threading import Thread, current_thread


# can pass in user dirs here
class S4Server (ServerInterface):
    def check_auth_password(self, username, password):
        if (self.current_user != username):
            print("preventing current_user: {}, log in user: {}".format(self.current_user, username))
            #return AUTH_FAILED
        self.logged_in_user = username
        print("setting logged in user: {}".format(username))
        self.logged_in_user_dir = self.user_dirs.get(username)
        return AUTH_SUCCESSFUL

    def check_auth_publickey(self, username, key):
        print("check_auth_publickey, username: {}, key: {}".format(username, key))
        return AUTH_FAILED

    def check_channel_request(self, kind, chanid):
        print("check_channel_request, kind: {}, chanid: {}".format(kind, chanid))
        return OPEN_SUCCEEDED

    def get_allowed_auths(self, username):
        return "password"

    def check_channel_shell_request(self, channel):
        print("check_channel_shell_request, channel: {}".format(channel))
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
        print("init S4SFTPServerHandler")
        self.server = server
        super().__init__(server, *args, **kwargs)

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

    def stat(self, path):
        if (self.server.connection_is_down):
            return SFTP_FAILURE
        try:
            return SFTPAttributes.from_stat(os.stat(self._realpath(path)))
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)

    def open(self, path, flags, attr):
        if (self.server.connection_is_down):
            return SFTP_FAILURE
        print("open {}, user: {}".format(path, self.server.logged_in_user))
        path = self._realpath(path)
        try:
            fd = os.open(path, flags, 0o666)
        except OSError as e:
            return SFTPServer.convert_errno(e.errno)
        if (flags & os.O_CREAT) and (attr is not None):
            attr._flags &= ~attr.FLAG_PERMISSIONS
            SFTPServer.set_file_attr(path, attr)

        if flags & os.O_WRONLY:
            fstr = 'wb'
        else:
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

class S4SFTPServer(object):

    ROBOT_LIBRARY_SCOPE = 'GLOBAL'

    def __init__(self):
        self.user_dirs = {}
        self.channels = []

    @keyword(types=['string'])
    def init_sftp_server(self, relative_ftp_dir):
        dir_path = os.path.dirname(os.path.realpath(__file__))
        self.ftp_dir = os.path.join(dir_path, '../../', relative_ftp_dir)
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
        host_key = paramiko.RSAKey.from_private_key_file("/tmp/robot_id_rsa")

        def serve_forever(self, server):
            keepServing = True
            while keepServing:
                conn, addr = server_socket.accept()
                transport = paramiko.Transport(conn)
                transport.add_server_key(host_key)
                self.server = S4Server()
                self.server.ftp_dir = self.ftp_dir
                self.server.user_dirs = self.user_dirs
                self.server.connection_is_down = False
                transport.set_subsystem_handler('sftp', paramiko.SFTPServer, S4SFTPServerHandler)
                transport.start_server(server=server)
                chan = transport.accept()
                print("adding a new channel {}".format(chan))
                self.channels.append(chan)
                #time.sleep(10)

        self.server = S4Server()
        self.server.ftp_dir = self.ftp_dir
        self.server.user_dirs = self.user_dirs
        self.server.connection_is_down = False
        self.ftp_thread = Thread(target=serve_forever, args=(self, self.server, ))
        self.ftp_thread.setDaemon(True)
        self.ftp_thread.start()

    @keyword(types=['string', 'string', 'string', 'list'])
    def add_sftp_user(self, user, password, dir_name, subdir_names):
        user_dir = os.path.join(self.ftp_dir, dir_name)
        if not os.path.exists(user_dir): os.makedirs(user_dir)
        for subdir_name in subdir_names:
            dir = os.path.join(self.ftp_dir, dir_name, subdir_name)
            if not os.path.exists(dir): os.makedirs(dir)
        self.user_dirs[user] = user_dir
        return user_dir

    @keyword(types=['string'])
    def get_sftp_dir_for(self, user):
        return self.user_dirs.get(user)

    @keyword(types=['string'])
    def set_current_sftp_user(self, user):
        self.server.current_user = user

    @keyword()
    def close_sftp_server(self):
        for c in self.channels: c.close()

    @keyword()
    def set_sftp_connection_as_down(self):
        self.server.connection_is_down = True

    @keyword()
    def set_sftp_connection_as_up(self):
        self.server.connection_is_down = False