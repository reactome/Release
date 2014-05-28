/*
 * Created on Sep 10, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gk.util;

import java.util.List;

/**
 * @author vastrik
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class StringUtils {

	/*
	 * A function mimicking PERL's function with the same name
	 */
	public static String join(String joinStr, List list) {
		if (list.size() == 0) return "";
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < list.size() - 1; i++) {
			buf.append(list.get(i).toString() + joinStr);
		}
		buf.append(list.get(list.size() - 1));
		return buf.toString();
	}

}
