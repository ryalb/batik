/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.css.engine.value;

import org.w3c.dom.css.CSSPrimitiveValue;

/**
 * This class represents float values.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class FloatValue extends AbstractValue {
    
    /**
     * The unit types representations
     */
    protected final static String[] UNITS = {
        "", "%", "em", "ex", "px", "cm", "mm", "in", "pt",
        "pc", "deg", "rad", "grad", "ms", "s", "Hz", "kHz", ""
    };

    /**
     * The float value
     */
    protected float floatValue;

    /**
     * The unit type
     */
    protected short unitType;

    /**
     * Creates a new value.
     */
    public FloatValue(short unitType, float floatValue) {
	this.unitType   = unitType;
	this.floatValue = floatValue;
    }

    /**
     * The type of the value.
     */
    public short getPrimitiveType() {
        return unitType;
    }

    /**
     * Returns the float value.
     */
    public float getFloatValue() {
        return floatValue;
    }

    /**
     *  A string representation of the current value. 
     */
    public String getCssText() {
        String s = "" + floatValue;
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
	return s + UNITS[unitType - CSSPrimitiveValue.CSS_NUMBER];
    }

    /**
     * Returns a printable representation of this value.
     */
    public String toString() {
        return getCssText();
    }
}