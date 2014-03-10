import cmd

class CmdInterpretor(cmd.Cmd):

    prompt = 'stratos>'

    def do_add(self, line):
        print 'command line intepretor working'

    def do_EOF(self, line):
        return True