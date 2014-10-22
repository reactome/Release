/*
 * Created on Oct 11, 2005
 *
 */
package org.reactome.core.controller;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReflectionUtility {
    public static final Object[] EMPTY_ARG = new Object[]{};

    public static String lowerFirst(String propName) {
        return propName.substring(0, 1).toLowerCase() + propName.substring(1);
    }

    public static String upperFirst(String propName) {
        return propName.substring(0, 1).toUpperCase() + propName.substring(1);
    }

    /**
     * A helper method to get the list of property names from a specified org.reactome.restfulapi.domain class.
     *
     * @param cls a Domain class
     * @return a list of property names. Note: the first letter is in upper case.
     */
    public static List<String> getJavaBeanProperties(Class cls) {
        Method[] methods = cls.getMethods();
        // Check method name
        String methodName = null;
        String propName = null;
        List<String> propNames = new ArrayList<String>();
        for (Method m : methods) {
            methodName = m.getName();
            if (methodName.startsWith("get")) {
                propName = methodName.substring(3);
                if (propName.length() > 0) {
                    // Have to lower the first case
                    propNames.add(propName);
                }
            }
        }
        return propNames;
    }

    /**
     * Get a named method from Class for the specified target object. This method is different from
     * Class.getMethod(), which cannot search through super class methods.
     *
     * @param target
     * @param methodName
     * @return
     * @throws Exception
     */
    public static Method getNamedMethod(Object target, String methodName) {
        Method[] methods = target.getClass().getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName))
                return m;
        }
        return null;
    }

    /**
     * Set the property value for a specified property via proprety name.
     *
     * @param target
     * @param propName
     * @param propValue
     * @throws java.lang.reflect.InvocationTargetException
     *
     * @throws IllegalAccessException
     */
    public static void setProperty(Object target, String propName, Object propValue)
            throws InvocationTargetException, IllegalAccessException {
        String tmp = upperFirst(propName);
        Method setPropMethod = getNamedMethod(target, "set" + tmp);
        if (setPropMethod != null)
            setPropMethod.invoke(target, new Object[]{propValue});
    }

    public static Object getProperty(Object target, String propName)
            throws InvocationTargetException, IllegalAccessException {
        String tmp = upperFirst(propName);
        Method getPropMethod = getNamedMethod(target, "get" + tmp);
        if (getPropMethod == null)
            return null;
        return getPropMethod.invoke(target, EMPTY_ARG);
    }

    public static Object generateProxy(Object obj) throws InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        Object proxy = obj.getClass().newInstance();
        //Handle ID
        Method method = getNamedMethod(obj, "getId");
        Object id = method.invoke(obj, EMPTY_ARG);
        method = getNamedMethod(proxy, "setId");
        method.invoke(proxy, new Object[]{id});
        //Handle name if applicable
        method = getNamedMethod(obj, "getName");
        // name property is not required always
        if (method != null) {
            Object name = method.invoke(obj, EMPTY_ARG);
            method = getNamedMethod(proxy, "setName");
            method.invoke(proxy, new Object[]{name});
        }
        return proxy;
    }

    /**
     * Get all fields for the specified Class object. However fields in Object is not
     * included.
     *
     * @param cls
     * @return
     * @throws SecurityException
     */
    public static Set<Field> getFields(Class cls) throws SecurityException {
        Set<Field> rtn = new HashSet<Field>();
        Class superCls = cls;
        Field[] fields = null;
        while (!superCls.equals(Object.class)) {
            fields = superCls.getDeclaredFields();
            if (fields != null)
                for (Field f : fields)
                    rtn.add(f);
            superCls = superCls.getSuperclass();
        }
        return rtn;
    }

}
