/* 
/  Copyright (C) 2009  Antti Alho - Sesca Innovations Ltd
/  
/  This file is part of SIP-Applet (www.sesca.com, www.purplescout.com)
/
/  This program is free software; you can redistribute it and/or
/  modify it under the terms of the GNU General Public License
/  as published by the Free Software Foundation; either version 2
/  of the License, or (at your option) any later version.
/
/  This program is distributed in the hope that it will be useful,
/  but WITHOUT ANY WARRANTY; without even the implied warranty of
/  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/  GNU General Public License for more details.
/
/  You should have received a copy of the GNU General Public License
/  along with this program; if not, write to the Free Software
/  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.sesca.misc;

import java.applet.Applet;

public class JavaVersion extends Applet
{
	private static final long serialVersionUID = 1L;

	private String m_ver;

	private String m_ven;

	public JavaVersion()
	{
		m_ver = System.getProperty("java.version");
		m_ven = System.getProperty("java.vendor");
	}

	public String getVersion()
	{
		return m_ver;
	}

	public String getVendor()
	{
		return m_ven;
	}
}
