//Dress up a native JS object as the specified type
//(which should be a Ceylon dynamic interface)
function dre$$(object, type, loc, stack) {
  //If it's already dressed up as the type, leave it alone
  if (is$(object, type))return object;
  //If it's already of another type, throw
  if (object===null || object===undefined)return object;
  if (object.$$ !== undefined && object.getT$all()[object.getT$name()].dynmem$===undefined) {
    if (loc===false)return object;
    throw new TypeError("Cannot modify the type of an object to "+qname$(type)+" at runtime " + loc);
  }
  //If it's frozen, throw because we won't be able to dress it up
  if (typeof(object)==='object' && Object.isFrozen(object)) {
    if (loc===false)return object;
    throw new Error("Cannot add Ceylon type information to a frozen object");
  }
  //If it's a TypeScript enum, accept number values and nothing else
  if (type.t.$$.$tsenum) {
    if (typeof(object)==='number') {
      return object;
    } else {
      if (loc===false)return object;
      throw new Error("Native object cannot be a TypeScript enum");
    }
  }
  function memberTypeIsDynamicInterface$(t) {
    if (t.t && t.t.dynmem$) {
      return t;
    }
    if (t.t==='u') {
      var c=0,st;
      for (var i=0; i<t.l.length;i++) {
        if (t.l[i].t.dynmem$) {
          c++;
          st=t.l[i];
        }
      }
      if (c===1) {
        return st;
      }
    }
    return undefined;
  }
  //Check members, non-invasively first
  var actual = typeof(object)==='object'?Object.getOwnPropertyNames(object):[];
  var sats = type.t.$$.prototype.getT$all();
  var tname=type.t.$$.T$name;
  if (type.a) {
    var otargs={};
    for (var t in type.a) {
      otargs[t]=type.a[t];
    }
  }
  var t_all=sats;
  if (stack===undefined) {
    stack=[object];
  } else if (stack.indexOf(object)<0) {
    stack.push(object);
  }
  for (var sat in sats) {
    var expected = sats[sat].dynmem$;
    if (expected) {
      for (var i=0; i < expected.length; i++) {
        var propname="$prop$get"+expected[i][0].uppercased+expected[i].substring(1);
        var proptype=type.t.$$.prototype[propname];
        if (proptype) {
          proptype=getrtmm$$(proptype);
        }
        if (actual.indexOf(expected[i])<0 && object[expected[i]]===undefined) {
          if (!(proptype && extendsType({t:Null},proptype.$t))) {
            if (loc===false)return object;
            throw new Error("Native object is missing property '" + expected[i] + "' " + loc);
          }
        } else {
          var val=object[expected[i]],dynmemberType;
          if (val===object) {
            //avoid instance circularity
            if (!is$(val,proptype.$t)) {
              //and make this an intersection type
              tname=tname+"|"+proptype.$t.t.$$.T$name;
              //Copy the satisfied types and add the new one
              var _ts={};
              for (var _tn in t_all) {
                _ts[_tn]=t_all[_tn];
              }
              _ts[proptype.$t.t.$$.prototype.getT$name()]=proptype.$t.t;
              t_all=_ts;
            }
          } else if (proptype && proptype.$t && !is$(val,proptype.$t)) {
            if (proptype.$t.t===$_Array) {
              if (natc$(val,proptype.$t.a.Element$Array,false,stack)===false)return object;
            } else if (proptype.$t.t===Integer) {
              if (ndnc$(val,'i',false)===false)return object;
            } else if (proptype.$t.t===Float) {
              if (ndnc$(val,'f',false)===false)return object;
            } else if ((dynmemberType=memberTypeIsDynamicInterface$(proptype.$t))!==undefined) {
              //If the member type is a dynamic interface, dress up the value
              if (stack.indexOf(val)<0) {
                dre$$(val,dynmemberType,loc,stack);
              }
            } else {
              var _t=proptype.$t;
              if (typeof(_t)==='string') {
                if (otargs[_t]) {
                  _t=otargs[_t];
                } else {
                  var mm=getrtmm$$(type.t);
                  if (mm && mm.sts) {
                    for (var i=0;i<mm.sts.length;i++) {
                      if (mm.sts[i].a && mm.sts[i].a[_t]) {
                        otargs[_t]=mm.sts[i].a[_t];
                        _t=mm.sts[i].a[_t]; break;
                      }
                    }
                  }
                }
              }
              if (ndtc$(val,_t,false)===false)return object;
            }
          }
        }
      }
    }
  }
  object.$$=type.t.$$;
  object.getT$name=function(){return tname}
  object.getT$all=function(){return t_all} 
  if (type.a) {
    object.$$targs$$=otargs;
  }
  for (var sat in sats) {
    var expected = sats[sat].dynmem$;
    if (expected) {
      for (var i=0; i < expected.length; i++) {
        var propname="$prop$get"+expected[i][0].uppercased+expected[i].substring(1);
        var proptype=type.t.$$.prototype[propname];
        if (proptype) {
          proptype=getrtmm$$(proptype);
        }
        if (actual.indexOf(expected[i])<0 && object[expected[i]]===undefined) {
          if (proptype && extendsType({t:Null},proptype.$t)) {
            object[expected[i]]=null;
          } else {
            if (loc===false)return object;
            throw new Error("Native object is missing property '" + expected[i] + "' " + loc);
          }
        } else {
          var val=object[expected[i]],dynmemberType;
          if (val===object) {
            //avoid instance circularity
            if (!is$(val,proptype.$t)) {
              //and make this an intersection type
              var tname=object.getT$name()+"|"+proptype.$t.t.$$.T$name;
              object.getT$name=function(){return tname;}
              //Copy the satisfied types and add the new one
              var _ts={};
              for (var _tn in object.getT$all()) {
                _ts[_tn]=object.getT$all()[_tn];
              }
              _ts[proptype.$t.t.$$.prototype.getT$name()]=proptype.$t.t;
              object.getT$all=function(){return _ts;}
              //type arguments
              object.$$=$_Object.$$;
            }
          } else if (proptype && proptype.$t && !is$(val,proptype.$t)) {
            if (proptype.$t.t===$_Array) {
              object[expected[i]]=natc$(val,proptype.$t.a.Element$Array,loc,stack);
            } else if (proptype.$t.t===Integer) {
              object[expected[i]]=ndnc$(val,'i',loc);
            } else if (proptype.$t.t===Float) {
              object[expected[i]]=ndnc$(val,'f',loc);
            } else if ((dynmemberType=memberTypeIsDynamicInterface$(proptype.$t))!==undefined) {
              //If the member type is a dynamic interface, dress up the value
              if (stack.indexOf(val)<0) {
                dre$$(val,dynmemberType,loc,stack);
              }
            } else {
              var _t=proptype.$t;
              if (typeof(_t)==='string') {
                if (object.$$targs$$[_t]) {
                  _t=object.$$targs$$[_t];
                } else {
                  var mm=getrtmm$$(type.t);
                  if (mm && mm.sts) {
                    for (var i=0;i<mm.sts.length;i++) {
                      if (mm.sts[i].a && mm.sts[i].a[_t]) {
                        object.$$targs$$[_t]=mm.sts[i].a[_t];
                        _t=mm.sts[i].a[_t]; break;
                      }
                    }
                  }
                }
              }
              object[expected[i]]=ndtc$(val,_t,loc);
            }
          }
        }
      }
    }
  }
  if (typeof(object)==='object') {
    if (actual.indexOf('string')<0) {
      atr$(object,'string',function(){return object.toString();},undefined,
           $_Object.$$.prototype.$prop$getString.$crtmm$);
    }
    if (actual.indexOf('hash')<0) {
      atr$(object,'hash',function(){return identityHash(object);},undefined,
           $_Object.$$.prototype.$prop$getHash.$crtmm$);
    }
  }
  return object;
}
ex$.dre$$=dre$$;
//print native dynamic object
function pndo$(o) {
  if (o === undefined)print("<undefined>");
  else if (o === null)print("<null>");
  else if (is$(o,{t:Anything}))print(o);
  else if (o.string)print(o.string);
  else {
    print(o.toString());
  }
}
ex$.pndo$=pndo$;
//check if numbers are really numbers
function ndnc$(n,t,loc) {
  if (t==='f') {
    if (typeof(n)==='number')return Float(n);
    if (is$(n,{t:Float}))return n;
  } else if (t==='i') {
    if (typeof(n)==='number')return Math.floor(n);
    if (is$(n,{t:Integer}))return n;
  }
  if (loc===false)return false;
  throw new TypeError('Expected ' + (t==='f'?'Float':'Integer') + ' (' + loc + ')');
}
ex$.ndnc$=ndnc$;
//Check if an object if really of a certain type
function ndtc$(o,t,loc) {
  if (is$(o,t))return o;
  if (loc===false)return false;
  throw new TypeError('Expected ' + qname$(t) + ' (' + loc + ')');
}
ex$.ndtc$=ndtc$;
//Box native array, checking elements' type
function natc$(a,t,loc,stack) {
  if (a===empty())return $arr$([],t);
  if (Array.isArray(a)) {
    for (var i=0;i<a.length;i++) {
      if (!is$(a[i],t) && (a[i] && a[i].$$)===undefined) {
        a[i]=dre$$(a[i],t,loc,stack);
      }
    }
    return $arr$(a,t);
  }
  if(loc===false)return false;
  throw new TypeError('Expected ' + qname$(t) + ' (' + loc + ')');
}
ex$.natc$=natc$;

//A special kind of "require" for non-standard npm modules
//that return a single function instead of a proper exports object
function npm$req(name,mod,req) {
  var x=req(mod);
  if (typeof(x)==='function') {
    var k=Object.keys(x);
    if (k.length===0) {
      var o={};
      o[name]=x;
      x=o;
    } else if (k.indexOf(name)<0) {
      x[name]=x;
    }
  }
  return x;
}
ex$.npm$req=npm$req;
//Functions for determining on which platform we run
function run$isBrowser() {
    return (typeof navigator !== "undefined") && (typeof window !== "undefined");
}
ex$.run$isBrowser=run$isBrowser;
function run$isNode() {
    return (typeof process !== "undefined");
}
ex$.run$isNode=run$isNode;
