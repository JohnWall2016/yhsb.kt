import sys, os


def usage():
    print("python3 cjb.py [-X] 应用名称 [应用参数]")
    exit(1)


if len(sys.argv) < 2:
    usage()

debug = True if sys.argv[1] == '-X' else False

if debug and len(sys.argv) == 2:
    usage()

main = f'cn.yhsb.cjb.application.{sys.argv[2]}' if debug else f'cn.yhsb.cjb.application.{sys.argv[1]}'
args = ' '.join(sys.argv[3:]) if debug else ' '.join(sys.argv[2:])
cmd = f'mvn exec:java {"-X" if debug else ""} -Dexec.mainClass={main} -q -Dexec.args="{args}"'
# print(cmd)
os.system(cmd)