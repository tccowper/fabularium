extern function test1;
extern function test2();
extern function test3(a);

main()
{
    test1();
    test2();
    test2(1);
    test3();
    test3(1);
}
