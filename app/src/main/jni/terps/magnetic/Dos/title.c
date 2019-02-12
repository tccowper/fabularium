
void showPNG(const char *, unsigned short, unsigned short);

int main(int argc, char** argv)
{
	if (argc == 2)
		showPNG(argv[1],0x110,0x111);
	return 0;
}

