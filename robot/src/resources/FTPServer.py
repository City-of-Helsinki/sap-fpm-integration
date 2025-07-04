from pyftpdlib import servers
from pyftpdlib.handlers import FTPHandler
from robot.api.deco import keyword, not_keyword
from threading import Thread, current_thread
from pyftpdlib.authorizers import DummyAuthorizer
import os


class FTPServer(object):

    ROBOT_LIBRARY_SCOPE = 'GLOBAL'

    def __init__(self):
        self.user_dirs = {}

    @keyword(types=['string'])
    def init_ftp_server(self, masquerade_address, relative_ftp_dir):
        address = ("0.0.0.0", 2121)
        self.ftp_dir = os.path.join(os.getcwd(), relative_ftp_dir)
        if not os.path.exists(self.ftp_dir): os.makedirs(self.ftp_dir)
        handler = FTPHandler
        handler.masquerade_address = masquerade_address
        handler.passive_ports = range(40000, 40007)
        handler.authorizer = DummyAuthorizer()
        self.server = servers.ThreadedFTPServer(address, handler)

    @keyword()
    def start_ftp_server(self):
        def serve_forever(server):
            with server: server.serve_forever()
        self.ftp_thread = Thread(target=serve_forever, args=(self.server, ))
        self.ftp_thread.setDaemon(True)
        self.ftp_thread.start()

    @keyword(types=['string', 'string', 'string', 'list'])
    def add_ftp_user(self, user, password, dir_name, subdir_names):
        user_dir = os.path.join(self.ftp_dir, dir_name)
        if not os.path.exists(user_dir): os.makedirs(user_dir)
        for subdir_name in subdir_names:
            dir = os.path.join(self.ftp_dir, dir_name, subdir_name)
            if not os.path.exists(dir): os.makedirs(dir)
        self.server.handler.authorizer.add_user(user, password, user_dir, perm="elradfmwMT")
        self.user_dirs[user] = user_dir
        return user_dir

    @keyword(types=['string'])
    def get_ftp_dir_for(self, user):
        return self.user_dirs.get(user)

    @keyword()
    def get_main_ftp_dir(self):
        return self.ftp_dir

    @keyword()
    def close_ftp_server(self):
        self.server.close_all()