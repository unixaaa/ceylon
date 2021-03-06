import check { check, fail }

class Issue306(String() callback) {
    shared void call() {
        check("other"==callback(), "Issue 306");
    }    
}
object objectIssue306 {
    String callback() {
        return other();
    }
    String other() {
        return "other";
    }
    shared Issue306 foo() {
        return Issue306(callback);
    }
}

class ClassBug314<T>(one=1, two="2") {
  shared Integer one;
  shared String two;
  check(one==1,"Issue 314 class");
}
void methodBug314<T>(Integer one=1, String two="2") {
  check(two=="2", "Issue 314 method");
}

R bar326tl<A, R>(A arg) {
  assert(is R r=arg);
  return r;
}
R(A) foo326tl<A, R>() => bar326tl<A,R>;

void testIssue326() {
  check(foo326tl<Integer,Number<Integer>>()(5)==5, "Issue 326 #1");
  value ftl = foo326tl<Integer,Number<Integer>>();
  check(ftl(5)==5, "Issue 326 #2");
  R bar326<A, R>(A arg) {
    assert(is R r=arg);
    return r;
  }
  R(A) foo326<A, R>() => bar326<A,R>;
  check(foo326<Integer,Number<Integer>>()(5)==5, "Issue 326 #3");
  value fi = foo326<Integer,Number<Integer>>();
  check(fi(5)==5, "Issue 326 #4");
}

variable Integer count376=0;

[Integer,Integer,Integer] ints376() {
  count376++;
  return [1,2,3];
}
String bug376_1(Object a, Object b, Object c) => "``a``, ``b``, ``c``";
Integer bug376_2(Object a, Object b, Object c, Integer d=1) => d+1;
Integer bug376_3(Object a, Object b, Integer c=1) => c+1;
Integer bug376_4(Integer a=2, Integer b=4, Integer c=6) => a+b+c;

void testIssue376() {
  check(bug376_1(*ints376())=="1, 2, 3", "#376 invoke simple");
  check(count376 == 1, "#376 params");
  check(bug376_2(*ints376())==2, "#376 invoke defaulted 1");
  check(bug376_3(*ints376())==4, "#376 invoke defaulted 2");
  check(bug376_4(*ints376())==6, "#376 invoke defaulted 3");
  value zeroes = [0,0,0];
  check(bug376_1(*zeroes)=="0, 0, 0", "#376 0's simple");
  check(bug376_2(*zeroes)==2, "#376 0's defaulted 1");
  check(bug376_3(*zeroes)==1, "#376 0's defaulted 2");
  check(bug376_4(*zeroes)==0, "#376 0's defaulted 3");
}

[Boolean,Boolean] i427(String expression, {Anything*} eval) =>
    [ eval is {String*}, eval is {Anything+} ];

void test427() {
    check(i427("{}", {}) == [true,false], "#427 1");
    check(i427{ "named <empty>"; } == [true, false], "#427 2");
    check(i427("{\"a\"}", {"a"}) == [true, true], "#427 3");
    value x = {"a"};
    check(i427("let x={\"a\"} in x", x) == [true, true], "#427 4");
    check(i427{ "named \"a\""; "a" } == [true, true], "#427 5");
}

object obj568 {
    shared List<B>(List<A>) lift<A, B>(B(A) f)
        =>  curry(map<A,B>)(f);

    shared List<B> map<A,B>(B(A) f, List<A> list)
        =>  list.collect(f); 

    shared List<Integer> double(List<Integer> list)
        =>  uncurry(List<Integer>.collect<Integer>)(list,2.times);
}

void test568() {
    value double = obj568.lift(2.times);
    check(double(Array {1,2,3}) == [2,4,6], "#568.1");
    check(double([1,2,3]) == [2,4,6], "#568.2");
    check(obj568.double([1,2,3]) == [2,4,6], "#568.3");   // error #2
}

interface Monad627<Box> {
    shared void join(Box source) {
        value x = identity<Box>;
        if (exists y=x(source)) {
            check(y==[[1]], "#627");
        } else {
            fail("#627.2");
        }
    }
}
object singleton627 satisfies Monad627<Singleton<Anything>> {}
void test627() {
    singleton627.join(Singleton(Singleton(1)));
}

void spreadIssues1(Integer* x) => check(x=={1,2,3},"spreadIssues1");
void spreadIssues2(Integer x, Integer* y) {
  check(x==1,"spreadIssues2.1");
  check(y=={2,3},"spreadIssues2.2");
}
void spreadIssues3([Integer, Integer, Integer] t) =>
  check(t==[1,2,3],"spreadIssues3");
void spreadIssues4({Integer*} x) => check(x=={1,2,3},"spreadIssues4");
void spreadIssues5<Args>(Anything(*Args) g, Args a)
    given Args satisfies Anything[]
  => g(*a);
void issue6066_1({Anything*} t) => check(t.sequence()==[1,2,3],"#6066.1 ``t``");
void issue6066_2({Anything*} t) => check(t.sequence()==[[1,2,3]],"#6066.2 ``t``");

void test6777() {
    variable value i = 10;
    {Integer+} ints = identity<{Integer+}> { i };
    check(sum(ints)==10, "#6777.1");
    i = 20;
    check(sum(ints)==20, "#6777.2");
}

void testIssues() {
  objectIssue306.foo().call();
  ClassBug314<Object>();
  methodBug314<Object>();
  value t314=[1,"2"];
  methodBug314<Object>(*t314);
  testIssue326();
  testIssue376();
  test427();
  test568();
  value test624 = [[1,"one"], [1, "two"]];
  check(1 in test624.group(Tuple.first).keys, "#624");
  test627();
  value t631=[[1,1]];
  check((t631.map<Integer[2]>((x)=>x).first of Object) is Integer[2], "#631.1");
  check((t631.map<Integer->Integer>((x)=>x[0]->x[1]).first of Object) is Integer->Integer, "#631.2");

  value triplet = [1,2,3];
  spreadIssues1(*triplet);
  spreadIssues2(*triplet);
  spreadIssues3(triplet);
  spreadIssues4(triplet);
  spreadIssues5(spreadIssues1,triplet);
  spreadIssues5(spreadIssues2,triplet);
  spreadIssues5(flatten(spreadIssues3),triplet);
  spreadIssues5(flatten(spreadIssues4),triplet);
  issue6066_1(triplet);
  issue6066_1({*triplet});
  issue6066_1([*triplet]);
  issue6066_1{*triplet};
  issue6066_2({triplet});
  issue6066_2([triplet]);
  issue6066_2{triplet};
  test6777();
}
