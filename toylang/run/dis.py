import subprocess

import os
files = [f for f in os.listdir('.') if os.path.isfile(f) and os.path.splitext(f)[-1] == '.bc']
for f in files:
    output = subprocess.check_output('llvm-dis {}'.format(f))
    print(output)
