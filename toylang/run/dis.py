import subprocess
from pathlib import Path
import os

for f in Path('.').glob('**/*.bc'):
    output = subprocess.check_output('llvm-dis {}'.format(f))
    print(output)
