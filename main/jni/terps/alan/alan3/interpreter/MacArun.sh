#!/bin/sh
cat > $TMPDIR/arun.command <<EOF
#!/bin/sh
arun $1
if [ \$? -ne 0 ]
then
    echo
    echo "<< Press enter to close this window >>"
    read
fi
EOF
chmod +x $TMPDIR/arun.command
open $TMPDIR/arun.command
#rm $TMPDIR/arun.command
