/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.css.dom;

import java.util.HashMap;
import java.util.Map;

import org.apache.batik.css.engine.value.Value;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSValue;

/**
 * This class represents a style declaration.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class CSSOMStyleDeclaration implements CSSStyleDeclaration {

    /**
     * The associated value.
     */
    protected ValueProvider valueProvider;

    /**
     * The modifications handler.
     */
    protected ModificationHandler handler;

    /**
     * The parent rule.
     */
    protected CSSRule parentRule;

    /**
     * The values.
     */
    protected Map values;

    /**
     * Creates a new style declaration.
     */
    public CSSOMStyleDeclaration(ValueProvider vp, CSSRule parent) {
        valueProvider = vp;
        parentRule = parent;
    }

    /**
     * Sets the modification handler of this value.
     */
    public void setModificationHandler(ModificationHandler h) {
        handler = h;
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#getCssText()}.
     */
    public String getCssText() {
        return valueProvider.getText();
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#setCssText(String)}.
     */
    public void setCssText(String cssText) throws DOMException {
	if (handler == null) {
            throw new DOMException
                (DOMException.NO_MODIFICATION_ALLOWED_ERR, "");
	} else {
            values = null;
            handler.textChanged(cssText);
	}
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#getPropertyValue(String)}.
     */
    public String getPropertyValue(String propertyName) {
        Value value = valueProvider.getValue(propertyName);
        if (value == null) {
            return "";
        }
        return value.getCssText();
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#getPropertyCSSValue(String)}.
     */
    public CSSValue getPropertyCSSValue(String propertyName) {
        Value value = valueProvider.getValue(propertyName);
        if (value == null) {
            return null;
        }
        return getCSSValue(propertyName);
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#removeProperty(String)}.
     */
    public String removeProperty(String propertyName) throws DOMException {
        String result = getPropertyValue(propertyName);
        if (result.length() > 0) {
            if (handler == null) {
                throw new DOMException
                    (DOMException.NO_MODIFICATION_ALLOWED_ERR, "");
            } else {
                if (values != null) {
                    values.remove(propertyName);
                }
                handler.propertyRemoved(propertyName);
            }
        }
        return result;
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#getPropertyPriority(String)}.
     */
    public String getPropertyPriority(String propertyName) {
        return (valueProvider.isImportant(propertyName)) ? "important" : "";
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#setProperty(String,String,String)}.
     */
    public void setProperty(String propertyName, String value, String prio)
	throws DOMException {
        if (handler == null) {
            throw new DOMException
                (DOMException.NO_MODIFICATION_ALLOWED_ERR, "");
        } else {
            handler.propertyChanged(propertyName, value, prio);
        }
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#getLength()}.
     */
    public int getLength() {
        return valueProvider.getLength();
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#item(int)}.
     */
    public String item(int index) {
        return valueProvider.item(index);
    }

    /**
     * <b>DOM</b>: Implements {@link
     * org.w3c.dom.css.CSSStyleDeclaration#getParentRule()}.
     */
    public CSSRule getParentRule() {
        return parentRule;
    }

    /**
     * Gets the CSS value associated with the given property.
     */
    protected CSSValue getCSSValue(String name) {
        CSSValue result = (CSSValue)values.get(name);
        if (result == null) {
            result = createCSSValue(name);
            values.put(name, result);
        }
        return result;
    }

    /**
     * Creates the CSS value associated with the given property.
     */
    protected CSSValue createCSSValue(String name) {
        return new StyleDeclarationValue(name);
    }

    /**
     * To provides the values.
     */
    public interface ValueProvider {

        /**
         * Returns the current value associated with this object.
         */
        Value getValue(String name);

        /**
         * Tells whether the given property is important.
         */
        boolean isImportant(String name);

        /**
         * Returns the text of the declaration.
         */
        String getText();

        /**
         * Returns the length of the declaration.
         */
        int getLength();

        /**
         * Returns the value at the given.
         */
        String item(int idx);

    }

    /**
     * To manage the modifications on a CSS value.
     */
    public interface ModificationHandler {

        /**
         * Called when the value text has changed.
         */
        void textChanged(String text) throws DOMException;

        /**
         * Called when a property was removed.
         */
        void propertyRemoved(String name) throws DOMException;

        /**
         * Called when a property was changed.
         */
        void propertyChanged(String name, String value, String prio)
            throws DOMException;

    }

    /**
     * This class represents a CSS value returned by this declaration.
     */
    protected class StyleDeclarationValue
        extends CSSOMValue
        implements CSSOMValue.ValueProvider {
        
        /**
         * The property name.
         */
        protected String property;

        /**
         * Creates a new StyleDeclarationValue.
         */
        public StyleDeclarationValue(String prop) {
            super(null);
            valueProvider = this;
            setModificationHandler(new AbstractModificationHandler() {
                    protected Value getValue() {
                        return StyleDeclarationValue.this.getValue();
                    }
                    public void textChanged(String text) throws DOMException {
                        if (values == null ||
                            values.get(this) == null ||
                            handler == null) {
                            throw new DOMException
                                (DOMException.NO_MODIFICATION_ALLOWED_ERR, "");
                        }
                        String prio = getPropertyPriority(property);
                        CSSOMStyleDeclaration.this.
                            handler.propertyChanged(property, text, prio);
                    }
                });

            property = prop;
        }

        // ValueProvider ///////////////////////////////

        /**
         * Returns the current value associated with this object.
         */
        public Value getValue() {
            return CSSOMStyleDeclaration.this.valueProvider.getValue(property);
        }

    }
}