package algocode.harness;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Converts parsed JSON test arguments into the Java types declared by the solution method. */
public final class ArgConverter {

    private ArgConverter() {
    }

    public static Object convert(Object json, Type target) {
        if (target instanceof Class<?> cls) {
            return convertToClass(json, cls);
        }
        if (target instanceof ParameterizedType pt) {
            Class<?> raw = (Class<?>) pt.getRawType();
            Type[] typeArgs = pt.getActualTypeArguments();
            if (Map.class.isAssignableFrom(raw)) {
                Map<Object, Object> map = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : asMap(json).entrySet()) {
                    Object key = convert(parseKey(e.getKey(), typeArgs[0]), typeArgs[0]);
                    map.put(key, convert(e.getValue(), typeArgs[1]));
                }
                return map;
            }
            if (Collection.class.isAssignableFrom(raw)) {
                Collection<Object> out = newCollection(raw);
                for (Object item : asList(json)) {
                    out.add(convert(item, typeArgs[0]));
                }
                return out;
            }
            return convert(json, raw);
        }
        if (target instanceof GenericArrayType gat) {
            List<?> list = asList(json);
            Type component = gat.getGenericComponentType();
            Class<?> rawComponent = rawClass(component);
            Object array = Array.newInstance(rawComponent, list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, convert(list.get(i), component));
            }
            return array;
        }
        if (target instanceof WildcardType wt) {
            return convert(json, wt.getUpperBounds()[0]);
        }
        return json;
    }

    private static Object convertToClass(Object json, Class<?> cls) {
        if (json == null) {
            if (cls.isPrimitive()) {
                throw new IllegalArgumentException("null cannot be passed as " + cls.getName());
            }
            return null;
        }
        if (cls == int.class || cls == Integer.class) {
            return number(json).intValue();
        }
        if (cls == long.class || cls == Long.class) {
            return number(json).longValue();
        }
        if (cls == double.class || cls == Double.class) {
            return number(json).doubleValue();
        }
        if (cls == float.class || cls == Float.class) {
            return number(json).floatValue();
        }
        if (cls == short.class || cls == Short.class) {
            return number(json).shortValue();
        }
        if (cls == byte.class || cls == Byte.class) {
            return number(json).byteValue();
        }
        if (cls == boolean.class || cls == Boolean.class) {
            return (Boolean) json;
        }
        if (cls == char.class || cls == Character.class) {
            if (json instanceof String s && s.length() == 1) {
                return s.charAt(0);
            }
            if (json instanceof Number n) {
                return (char) n.intValue();
            }
            throw new IllegalArgumentException("Cannot convert " + json + " to char");
        }
        if (cls == String.class) {
            return String.valueOf(json);
        }
        if (cls.isArray()) {
            List<?> list = asList(json);
            Class<?> component = cls.getComponentType();
            Object array = Array.newInstance(component, list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, convertToClass(list.get(i), component));
            }
            return array;
        }
        if (Collection.class.isAssignableFrom(cls)) {
            Collection<Object> out = newCollection(cls);
            out.addAll(asList(json));
            return out;
        }
        return json;
    }

    private static Collection<Object> newCollection(Class<?> raw) {
        if (Set.class.isAssignableFrom(raw)) {
            return new HashSet<>();
        }
        if (raw == LinkedList.class) {
            return new LinkedList<>();
        }
        if (Queue.class.isAssignableFrom(raw) && !List.class.isAssignableFrom(raw)) {
            return new ArrayDeque<>();
        }
        return new ArrayList<>();
    }

    private static Object parseKey(Object key, Type keyType) {
        String s = String.valueOf(key);
        Class<?> raw = rawClass(keyType);
        if (raw == Integer.class || raw == Long.class || raw == Short.class || raw == Byte.class) {
            return Long.parseLong(s);
        }
        if (raw == Double.class || raw == Float.class) {
            return Double.parseDouble(s);
        }
        return s;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType pt) {
            return (Class<?>) pt.getRawType();
        }
        if (type instanceof GenericArrayType gat) {
            return Array.newInstance(rawClass(gat.getGenericComponentType()), 0).getClass();
        }
        return Object.class;
    }

    private static Number number(Object json) {
        if (json instanceof Number n) {
            return n;
        }
        throw new IllegalArgumentException("Expected a number but got " + json);
    }

    private static List<?> asList(Object json) {
        if (json instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Expected an array but got " + json);
    }

    private static Map<?, ?> asMap(Object json) {
        if (json instanceof Map<?, ?> map) {
            return map;
        }
        throw new IllegalArgumentException("Expected an object but got " + json);
    }
}
