all: pre3.0alpha5.a3c pre3.0beta2.a3c pre3.0beta3.a3c pre3.0beta5.a3c pre3.0alpha5deb.a3c pre3.0beta2deb.a3c pre3.0beta3deb.a3c pre3.0beta5deb.a3c


pre3.0alpha5.a3c : versions.alan
	./alan3.0alpha4 versions.alan
	mv versions.a3c pre3.0alpha5.a3c

pre3.0beta2.a3c : versions.alan
	./alan3.0alpha9 versions.alan
	mv versions.a3c pre3.0beta2.a3c

pre3.0beta3.a3c : versions.alan
	./alan3.0beta2 versions.alan
	mv versions.a3c pre3.0beta3.a3c

pre3.0beta5.a3c : versions.alan
	./alan3.0beta4 versions.alan
	mv versions.a3c pre3.0beta5.a3c

# Debug versions
pre3.0alpha5deb.a3c : versions.alan
	./alan3.0alpha4 -debug versions.alan
	mv versions.a3c pre3.0alpha5deb.a3c

pre3.0beta2deb.a3c : versions.alan
	./alan3.0alpha9 -debug versions.alan
	mv versions.a3c pre3.0beta2deb.a3c

pre3.0beta3deb.a3c : versions.alan
	./alan3.0beta2 -debug versions.alan
	mv versions.a3c pre3.0beta3deb.a3c

pre3.0beta5deb.a3c : versions.alan
	./alan3.0beta4 -debug versions.alan
	mv versions.a3c pre3.0beta5deb.a3c
