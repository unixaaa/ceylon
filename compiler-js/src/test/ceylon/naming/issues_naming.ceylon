import check { check,fail }

shared object topObject483 {
  string = "object.";
}
shared Object getTopObject483() {
  return object {
    string = "method.";
  };
}

shared String topValue483 = "value.";
shared String getTopValue483() => "method.";

shared class Test483() {
  shared object member483 {
    string="object.";
  }
  shared Object getMember483() {
    return object {
      string="method.";
    };
  }
  shared String other="value.";
  shared String getOther() => "method.";
  check(member483.string=="object.", "#483.1 member (inside) ``member483``");
  check(getMember483().string=="method.", "#483.2 member (inside) ``getMember483()``");
  check(other=="value.", "#483.3 member (inside) ``other``");
  check(getOther()=="method.", "#483.4 member (inside) ``getOther()``");
}

variable String i6322 = "1";
variable String \iI6322 = "2";
void setI6322(String s) {
  fail("#6322.1");
}

void issues() {
  check(topObject483.string=="object.", "#483.1 toplevel ``topObject483``");
  check(getTopObject483().string=="method.", "#483.2 toplevel ``getTopObject483()``");
  check(topValue483=="value.", "#483.3 toplevel ``topValue483``");
  check(getTopValue483()=="method.", "#483.4 toplevel ``getTopValue483()``");

  object nestedObject483 {
    string="object.";
  }
  Object getNestedObject483() {
    return object {
      string="method.";
    };
  }
  check(nestedObject483.string=="object.", "#483.1 in method ``nestedObject483``");
  check(getNestedObject483().string=="method.", "#483.2 in method ``getNestedObject483()``");

  String nestedValue483="value.";
  String getNestedValue483() => "method.";
  check(nestedValue483=="value.", "#483.3 in method ``nestedValue483``");
  check(getNestedValue483()=="method.", "#483.4 in method ``getNestedValue483()``");

  value t=Test483();
  check(t.member483.string=="object.", "#483.1 member (outside) ``t.member483``");
  check(t.getMember483().string=="method.", "#483.2 member (outside) ``t.getMember483()``");
  check(t.other=="value.", "#483.3 member (outside) ``t.other``");
  check(t.getOther()=="method.", "#483.4 member (outside) ``t.getOther()``");

  check(i6322=="1", "#6322.2");
  check(\iI6322=="2", "#6322.3");
  i6322="ok1";
  \iI6322="ok2";
  check(i6322=="ok1", "#6322.4");
  check(\iI6322=="ok2", "#6322.5");
  //#6343
  check(parseFloat("1") exists, "#6343.1");
  check(isFinite()=="our own impl", "#6343.2");
}

shared String isFinite() => "our own impl";
