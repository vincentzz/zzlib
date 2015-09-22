package zzlib.functional;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Vincent on 15/9/18.
 * virtualzz0319@gmail.com
 */
public abstract class ZFunction<T> implements Cloneable {
    static SoftReference<Map<ZFunction, Object>> cache = new SoftReference<Map<ZFunction, Object>>(new ConcurrentHashMap<ZFunction, Object>());
    static Map<Class,ZFunction> functions = new HashMap<Class, ZFunction>();

    public List<Class> signature;
    public List<Object> args;
    public boolean useCache = true;


    public ZFunction(Class... clzs) {
        this.signature = new ArrayList<Class>(Arrays.asList(clzs));
        this.args = new ArrayList<Object> ();
        functions.put(this.getClass(), this);
    }

    private void setArgs(List<Class> signature, List<Object> appliedArgs) {
        this.signature = signature;
        this.args = appliedArgs;
    }

    public ZFunction<T> apply(final Object... args) {
        List<Class> newSignature = new ArrayList<Class>(this.signature);
        List<Object> newArgs = new ArrayList<Object>(this.args);
        for (Object arg : args) {
            Class clz = newSignature.get(0);
            newArgs.add(clz.cast(arg));
            newSignature.remove(0);
        }
        ZFunction<T> newFunc;
        try {
            newFunc = (ZFunction<T>) this.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
        newFunc.setArgs(newSignature, newArgs);
        return newFunc;
    }

    public T getValue(){
        T result;
        if (signature.isEmpty()) {
            if (useCache) {
                if (cache.get() == null) {
                    cache = new SoftReference<Map<ZFunction, Object>>(new ConcurrentHashMap<ZFunction, Object>());
                }
                if (cache.get().containsKey(this)) {
                    result = (T) (cache.get().get(this));
                } else {
                    result = body(args.toArray());
                    cache.get().put(this, result);
                }

            } else {
                result = body(args.toArray());
            }

            return result;
        } else throw new RuntimeException("Parameter is need to be applied first" + signature.toString());
    }

    abstract public T body(Object[] args);

    @Override
    public String toString() {
        return "Signature: " + signature.toString() + "\nArgs: " + args.toString();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException  {
        return super.clone();
    }

    @Override
    public boolean equals (Object obj){
        boolean result = false;
        if( obj instanceof ZFunction ) {
            ZFunction that = (ZFunction)obj;
            if ( this.signature.equals(that.signature) && this.args.equals(that.args)) {
                try {
                    result = this.getClass().getMethod("body", Object[].class).equals(that.getClass().getMethod("body", Object[].class));
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        try {
            int hashCode =this.getClass().getMethod("body", Object[].class).hashCode() + this.signature.hashCode() + this.args.hashCode();
            return hashCode;
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static void main(String[] args) {
        ZFunction test = new ZFunction(String.class, Integer.class) {
            @Override
            public Object body(Object[] args) {
                System.out.println("function is triggered");
                return Arrays.asList(args);
            }
        };

        ZFunction<List> map = new ZFunction<List>(ZFunction.class ,Collection.class) {
            @Override
            public List body(Object[] args) {
                ZFunction func = (ZFunction)args[0];
                Collection collection =(Collection) args[1];

                if ( func.signature.size()==1 ) {
                    Class clz = (Class) func.signature.get(0);
                    List result = new LinkedList();
                    for (Object item : collection) {
                        result.add(func.apply(clz.cast(item)).getValue());
                    }
                    return result;
                }else {
                    throw new RuntimeException("function can have only one argument.");
                }
            }
        };

        ZFunction<Integer> plus = new ZFunction<Integer>(Integer.class, Integer.class) {
            @Override
            public Integer body(Object[] args) {
                System.out.println("runned");
                return (Integer)args[0] + (Integer)args[1];
            }
        };

        final ZFunction foldr = new ZFunction(ZFunction.class, Collection.class) {
            @Override
            public Object body(Object[] args) {
                ZFunction f = (ZFunction) args[0];

                LinkedList l;
                if ( args[1] instanceof LinkedList) {
                    l = (LinkedList)args[1];
                } else {
                    l = new LinkedList();
                    l.addAll((Collection) args[1]);
                }

                Object result;
                if (l.isEmpty()) result = null;
                else if (l.size()==1) result = l.get(0);
                else {
                    try {
                        ZFunction foldr = functions.get(this.getClass());
                        Class p1 = (Class)f.signature.get(0);
                        Class p2 = (Class)f.signature.get(1);

                        result = f.apply(p1.cast(l.removeFirst()), p2.cast(foldr.apply(f, l).getValue())).getValue();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return result;
            }
        };


        System.out.println(test.apply("test").apply(123).equals(test.apply("test").apply(123)));
        System.out.println(test.apply("test").apply(123).getValue());
        System.out.println(test.apply("test").apply(123).getValue());
        System.out.println(test.apply("test", 123).getValue());

        List<Integer> intList = new LinkedList<Integer>();
        intList.add(1);
        intList.add(2);
        intList.add(3);

        System.out.println(map.apply(plus.apply(1), intList).getValue());
        System.out.println(plus.apply(1).apply(1).getValue());
        System.out.println(map.apply(plus.apply(1)).apply(intList).getValue().toString());

        System.out.println(plus.apply(1, 2).getValue().toString());

        System.out.println(foldr.apply(plus,intList).getValue().toString());
    }
}
