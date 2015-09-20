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

    public List<Class> signature;
    public List<Object> args;
    public boolean useCache = true;


    public ZFunction(Class... clzs) {
        this.signature = new ArrayList<Class>(Arrays.asList(clzs));
        this.args = new ArrayList<Object> ();
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

        ZFunction map = new ZFunction<List>(ZFunction.class ,Collection.class) {
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

        ZFunction plus = new ZFunction<Integer>(Integer.class) {
            @Override
            public Integer body(Object[] args) {
                System.out.println("runned");
                return (Integer)args[0] + 1;
            }
        };

        System.out.println(test.apply("test").apply(123).equals(test.apply("test").apply(123)));
        System.out.println(test.apply("test").apply(123).getValue());
        System.out.println(test.apply("test").apply(123).getValue());
        System.out.println(test.apply("test", 123).getValue());

        List<Integer> intList = new ArrayList<Integer>();
        intList.add(1);
        intList.add(2);
        intList.add(3);

        System.out.println(map.apply(plus, intList).getValue());
        System.out.println(plus.apply(1).getValue());
        System.out.println(map.apply(plus).apply(intList).getValue().toString());
    }
}
