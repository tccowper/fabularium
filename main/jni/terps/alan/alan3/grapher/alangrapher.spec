# -*- mode: python -*-

# Package with:
#   $ pyinstaller -F -w alangrapher.spec
#
# Beware of:
#   $ pyinstaller alangrapher.py
#
# since that overwrites this file

# On windows we need to specify the exe-extension, of course
extra_binaries = [("alan.exe", "c:\\Users\\Thomas\\Documents\\Utveckling\\Alan\\alan\\bin\\alan.exe", "BINARY")]

a = Analysis(['alangrapher.py'],
             pathex=['c:\\Users\\Thomas\\Documents\\Utveckling\\Alan\\alan\\grapher'],
             hiddenimports=[],
             hookspath=None,
             runtime_hooks=None)
pyz = PYZ(a.pure)
exe = EXE(pyz,
          a.scripts,
          a.binaries + extra_binaries,
          a.zipfiles,
          a.datas,
          name='alangrapher.exe',
          debug=False,
          strip=None,
          upx=True,
          console=False )
