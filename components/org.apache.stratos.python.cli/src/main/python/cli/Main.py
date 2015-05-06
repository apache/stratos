import sys
import CLI


def main():
    cli = CLI()
    if len(sys.argv) > 1:
        cli.onecmd(' '.join(sys.argv[1:]))
    else:
        cli.cmdloop()

if __name__ == '__main__':
    main()
