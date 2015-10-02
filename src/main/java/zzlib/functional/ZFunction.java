
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
		Map<ZFunction, Object> cacheMap = cache.get();
                if (cacheMap == null) {
		    cacheMap = new ConcurrentHashMap<ZFunction, Object>();
		    cache = new SoftReference<Map<ZFunction, Object>>(cacheMap);
                }
                if (cacheMap.containsKey(this)) {
                    result = (T) (cacheMap.get(this));
                } else {
                    result = body(args.toArray());
                    cacheMap.put(this, result);
                }
            } else {
                result = body(args.toArray());
            }
            return result;
        } else throw new RuntimeException("All parameters are need to be applied before calling getValue()." + signature.toString());
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
 
    /*
     * define  useful Functions
     */
    public static final ZFunction foldr = new ZFunction(ZFunction.class, Object.class, Collection.class) {
        @Override
        public Object body(Object[] args) {
            ZFunction f = (ZFunction) args[0];
            Object v = args[1];

            LinkedList l = new LinkedList();
            l.addAll((Collection) args[2]);

	    return l.isEmpty()? v : f.apply(l.removeFirst(), foldr.apply(f, v, l).getValue()).getValue(); 
	}
    };

    public static final ZFunction<Integer> plus = new ZFunction<Integer>(Integer.class, Integer.class) {
	@Override
	public Integer body(Object[] args) {
	    return (Integer)args[0] + (Integer)args[1];
	}
    };


    public static final ZFunction<Integer> sum = foldr.apply(plus, 0);

    public static final ZFunction<List> foldMap = new ZFunction<List>(ZFunction.class ,Collection.class) {
        @Override
        public List body(Object[] args) {
            final ZFunction f = (ZFunction) args[0];
            LinkedList l = new LinkedList();
            l.addAll((Collection) args[1]);

            return (List) foldr.apply(new ZFunction<List>(Object.class, LinkedList.class) {
                @Override
                public List body(Object[] args) {
		    Object x = args[0];
                    LinkedList xs =(LinkedList)args[1];
                    xs.addFirst(f.apply(x).getValue());
                    return xs;
                }
            }, new LinkedList(), l).getValue();
        }
    };

    public static final ZFunction<List> foldFilter = new ZFunction<List>(ZFunction.class ,Collection.class) {
        @Override
        public List body(Object[] args) {
            final ZFunction<Boolean> f = (ZFunction<Boolean>) args[0];
            LinkedList l = new LinkedList();
            l.addAll((Collection) args[1]);

            return (List) foldr.apply(new ZFunction<List>(Object.class, LinkedList.class) {
                @Override
                public List body(Object[] args) {
		    Object x = args[0];
                    LinkedList xs =(LinkedList)args[1];
                    if (f.apply(x).getValue()) {
			xs.addFirst(x);
		    }
                    return xs;
                }
            }, new LinkedList(), l).getValue();
        }
    };

    public static final ZFunction<List> map = new ZFunction<List>(ZFunction.class ,Collection.class) {
        @Override
        public List body(Object[] args) {
            ZFunction func = (ZFunction)args[0];
            Collection collection =(Collection) args[1];

	    List result = new LinkedList();
	    for (Object item : collection) {
		result.add(func.apply(item).getValue());
	    }
	    return result;
        }
    };

    public static final ZFunction<List> filter = new ZFunction<List> (ZFunction.class, Collection.class) {
	@Override
	public List body(Object[] args){
	    ZFunction<Boolean> f = (ZFunction<Boolean>) args[0];
	    LinkedList result = new LinkedList();
	    for ( Object item : (Collection) args[1]) {
		if (f.apply(item).getValue()){
		    result.add(item);
		}
	    }
	    return result;
	}
    };

    public static void main(String[] args) {
        List<Integer> intList = new LinkedList<Integer>();
        intList.add(1);
        intList.add(2);
        intList.add(3);

        System.out.println(map.apply(plus.apply(1), intList).getValue());
        System.out.println(plus.apply(1).apply(1).getValue());
        System.out.println(map.apply(plus.apply(1)).apply(intList).getValue().toString());

        System.out.println(foldMap.apply(plus.apply(1)).apply(intList).getValue().toString());

        System.out.println(plus.apply(1, 2).getValue().toString());

	System.out.println(foldFilter.apply(new ZFunction<Boolean>(Integer.class) {
                @Override
                public Boolean body(Object[] args) {
		    return ((Integer)args[0]) % 2 == 1;
                }
		}).apply(intList).getValue().toString());

	System.out.println(filter.apply(new ZFunction<Boolean>(Integer.class) {
                @Override
                public Boolean body(Object[] args) {
		    return ((Integer)args[0]) % 2 == 1;
                }
		}).apply(intList).getValue().toString());

        //System.out.println(foldr.apply(plus,intList).getValue().toString());
    }
}
