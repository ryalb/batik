/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.bridge;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.SampleModel;
import java.util.Map;
import java.util.Hashtable;
import java.lang.ref.SoftReference;

import org.apache.batik.ext.awt.image.GraphicsUtil;
import org.apache.batik.ext.awt.image.spi.ImageTagRegistry;
import org.apache.batik.ext.awt.image.renderable.Filter;

import org.apache.batik.css.engine.SVGCSSEngine;
import org.apache.batik.css.engine.value.ListValue;
import org.apache.batik.css.engine.value.StringValue;
import org.apache.batik.css.engine.value.Value;

import org.apache.batik.dom.util.XLinkSupport;
import org.apache.batik.dom.svg.XMLBaseSupport;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.ParsedURL;
import org.apache.batik.util.SoftReferenceCache;

import org.w3c.dom.Element;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;


/**
 * The CursorManager class is a helper class which preloads the cursors 
 * corresponding to the SVG built in cursors.
 *
 * @author <a href="mailto:vincent.hardy@sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public class CursorManager implements SVGConstants, ErrorConstants {
    /**
     * Maps SVG Cursor Values to Java Cursors
     */
    protected static Map cursorMap;

    /**
     * Default cursor when value is not found
     */
    public static final Cursor DEFAULT_CURSOR 
        = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

    /**
     * Cursor used over anchors
     */
    public static final Cursor ANCHOR_CURSOR
        = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    /**
     * Cursor used over text
     */
    public static final Cursor TEXT_CURSOR
        = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);

    /**
     * Static initialization of the cursorMap
     */
    static {
        cursorMap = new Hashtable();
        cursorMap.put(SVG_CROSSHAIR_VALUE,
                      Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        cursorMap.put(SVG_DEFAULT_VALUE,
                      Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        cursorMap.put(SVG_POINTER_VALUE,
                      Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cursorMap.put(SVG_MOVE_VALUE,
                      Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        cursorMap.put(SVG_E_RESIZE_VALUE,
                      Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        cursorMap.put(SVG_NE_RESIZE_VALUE,
                      Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
        cursorMap.put(SVG_NW_RESIZE_VALUE,
                      Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
        cursorMap.put(SVG_N_RESIZE_VALUE,
                      Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
        cursorMap.put(SVG_SE_RESIZE_VALUE,
                      Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        cursorMap.put(SVG_SW_RESIZE_VALUE,
                      Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
        cursorMap.put(SVG_S_RESIZE_VALUE,
                      Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
        cursorMap.put(SVG_W_RESIZE_VALUE,
                      Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
        cursorMap.put(SVG_TEXT_VALUE,
                      Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        cursorMap.put(SVG_WAIT_VALUE,
                      Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        cursorMap.put(SVG_HELP_VALUE, 
                      Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));  
        
    }

    /**
     * BridgeContext associated with this CursorManager
     */
    protected BridgeContext ctx;

    /**
     * Cache used to hold references to cursors
     */
    protected CursorCache cursorCache = new CursorCache();

    /**
     * Constructor
     *
     * @param BridgeContext ctx, the BridgeContext associated to this CursorManager
     */
    public CursorManager(BridgeContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Returns a Cursor object for a given cursor value. This initial implementation
     * does not handle user-defined cursors, so it always uses the cursor at the 
     * end of the list
     */
    public static Cursor getPredefinedCursor(String cursorName){
        return (Cursor)cursorMap.get(cursorName);
    }

    /**
     * Returns the Cursor corresponding to the input element's cursor property
     *
     * @param e the element on which the cursor property is set
     */
    public Cursor convertCursor(Element e) {
        Value cursorValue 
            = CSSUtilities.getComputedStyle(e, 
                                            SVGCSSEngine.CURSOR_INDEX);

        String cursorStr = SVGConstants.SVG_AUTO_VALUE;

        if (cursorValue != null) {
            if (cursorValue.getCssValueType() ==  CSSValue.CSS_PRIMITIVE_VALUE
                &&
                cursorValue.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                // Single Value : should be one of the predefined cursors or 
                // 'inherit'
                cursorStr = cursorValue.getStringValue();
                return convertBuiltInCursor(e, cursorStr);
            } else if (cursorValue.getCssValueType() == CSSValue.CSS_VALUE_LIST) {
                ListValue l = (ListValue)cursorValue;
                int nValues = l.getLength();
                if (nValues == 1) {
                    cursorValue = l.item(nValues-1);
                    if (cursorValue.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                        cursorStr = cursorValue.getStringValue();
                        return convertBuiltInCursor(e, cursorStr);
                    }
                } else if (nValues > 1) {
                    //
                    // Look for the first cursor url we can handle.
                    // That would be a reference to a <cursor> element.
                    //
                    return convertSVGCursor(e, l);
                }
            }
        } 
        
        return convertBuiltInCursor(e, cursorStr);
    }
    
    public Cursor convertBuiltInCursor(Element e, String cursorStr) {
        Cursor cursor = null;

        // The CSS engine guarantees an non null, non empty string
        // as the computed value for cursor. Therefore, the following
        // test is safe.
        if (cursorStr.charAt(0) == 'a') { 
            //
            // Handle 'auto' value.
            //
            // - <a> The following sets the cursor for <a> element enclosing
            //   text nodes. Setting the proper cursor (i.e., depending on the
            //   children's 'cursor' property, is handled in the SVGAElementBridge
            //   so as to avoid going up the tree on mouseover events (looking for
            //   an anchor ancestor.
            //
            // - <image> The following does not change the cursor if the 
            //   element's cursor property is set to 'auto'. Otherwise, it takes
            //   precedence over any child (in case of SVG content) cursor setting.
            //   This means that for images referencing SVG content, a cursor 
            //   property set to 'auto' on the <image> element will not override 
            //   the cursor settings inside the SVG image. Any other cursor property
            //   will take precedence.
            //
            // - <use> Same behavior as for <image> except that the behavior 
            //   is controlled from the <use> element bridge (SVGUseElementBridge).
            //
            // - <text>, <tref> and <tspan> : a cursor value of auto will cause the
            //   cursor to be set to a text cursor. Note that text content with an
            //   'auto' cursor and descendant of an anchor will have its cursor
            //   set to the anchor cursor through the SVGAElementBridge.
            //
            String nameSpaceURI = e.getNamespaceURI();
            String tag = e.getNodeName();
            if (SVGConstants.SVG_NAMESPACE_URI.equals(nameSpaceURI)) {
                if (SVGConstants.SVG_A_TAG.equals(tag)) {
                    cursor = CursorManager.ANCHOR_CURSOR;
                } else if (SVGConstants.SVG_TEXT_TAG.equals(tag) ||
                           SVGConstants.SVG_TSPAN_TAG.equals(tag) ||
                           SVGConstants.SVG_TREF_TAG.equals(tag) ) {
                    cursor = CursorManager.TEXT_CURSOR;
                } else if (SVGConstants.SVG_IMAGE_TAG.equals(tag)) {
                    // Do not change the cursor 
                    return null;
                } else {
                    cursor = CursorManager.DEFAULT_CURSOR;
                }
            } else {
                cursor = CursorManager.DEFAULT_CURSOR;
            }
        } else {
            // Specific, logical cursor
            cursor = CursorManager.getPredefinedCursor(cursorStr);
        }
        
        return cursor;
    }

       
    /**
     * Returns a cursor for the given value list. Note that the 
     * code assumes that the input value has at least two entries.
     * So the caller should check that before calling the method.
     * For example, CSSUtilities.convertCursor performs that check.
     */
    public Cursor convertSVGCursor(Element e, ListValue l) {
        int nValues = l.getLength();
        Element cursorElement = null;
        for (int i=0; i<nValues-1; i++) {
            Value cursorValue = l.item(i);
            if (cursorValue.getPrimitiveType() == CSSPrimitiveValue.CSS_URI) {
                String uri = cursorValue.getStringValue();
                
                // If the uri does not resolve to a cursor element,
                // then, this is not a type of cursor uri we can handle:
                // go to the next or default to logical cursor
                try {
                    cursorElement = ctx.getReferencedElement(e, uri);
                } catch (BridgeException be) {
                    // Be only silent if this is a case where the target
                    // could not be found. Do not catch other errors (e.g,
                    // malformed URIs)
                    if (!ERR_URI_BAD_TARGET.equals(be.getCode())) {
                        throw be;
                    }
                }
                
                if (cursorElement != null) {
                    // We go an element, check it is of type cursor
                    String cursorNS = cursorElement.getNamespaceURI();
                    if (SVGConstants.SVG_NAMESPACE_URI.equals(cursorNS)
                        &&
                        SVGConstants.SVG_CURSOR_TAG.equals(cursorElement.getNodeName())) {
                        Cursor c = convertSVGCursorElement(cursorElement);
                        if (c != null) { 
                            return c;
                        }
                    }
                }
            }
        }
        
        // If we got to that point, it means that no cursorElement
        // produced a valid cursor, i.e., either a format we support
        // or a valid referenced image (no broken image).
        // Fallback on the built in cursor property.
        Value cursorValue = l.item(nValues-1);
        String cursorStr = SVGConstants.SVG_AUTO_VALUE;
        if (cursorValue.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
            cursorStr = cursorValue.getStringValue();
        }
          
        return convertBuiltInCursor(e, cursorStr);
    }

    /**
     * Returns a cursor for a given element
     */
    public Cursor convertSVGCursorElement(Element cursorElement) {
        // One of the cursor url resolved to a <cursor> element
        // Try to handle its image.
        String uriStr = XLinkSupport.getXLinkHref(cursorElement);
        String baseURI = XMLBaseSupport.getCascadedXMLBase(cursorElement);
        ParsedURL purl;
        if (baseURI == null) {
            purl = new ParsedURL(uriStr);
        } else {
            purl = new ParsedURL(baseURI, uriStr);
        }

        //
        // Convert the cursor's hot spot
        //
        UnitProcessor.Context uctx 
            = UnitProcessor.createContext(ctx, cursorElement);

        String s = cursorElement.getAttributeNS(null, SVG_X_ATTRIBUTE);
        float x = 0;
        if (s.length() != 0) {
            x = UnitProcessor.svgHorizontalCoordinateToUserSpace
                (s, SVG_X_ATTRIBUTE, uctx);
        }

        s = cursorElement.getAttributeNS(null, SVG_Y_ATTRIBUTE);
        float y = 0;
        if (s.length() != 0) {
            y = UnitProcessor.svgVerticalCoordinateToUserSpace
                (s, SVG_Y_ATTRIBUTE, uctx);
        }

        CursorDescriptor desc = new CursorDescriptor(purl, x, y);

        // Check if there is a cursor in the cache for this url
        Cursor cachedCursor = cursorCache.getCursor(desc);

        if (cachedCursor != null) {
            return cachedCursor;
        } 
        
        ImageTagRegistry reg = ImageTagRegistry.getRegistry();
        Filter f = reg.readURL(purl);


        //
        // Check if we got a broken image
        //
        if (f.getProperty
            (SVGBrokenLinkProvider.SVG_BROKEN_LINK_DOCUMENT_PROPERTY) != null) {
            cursorCache.clearCursor(desc);
            return null;
        } 
            
        //
        // Now, get the preferred cursor dimension
        //
        Rectangle preferredSize = f.getBounds2D().getBounds();
        if (preferredSize == null || preferredSize.width <=0
            || preferredSize.height <=0 ) {
            cursorCache.clearCursor(desc);
            return null;
        }

        Dimension cursorSize 
            = Toolkit.getDefaultToolkit().getBestCursorSize
            (preferredSize.width, preferredSize.height);

        // 
        // Fit the rendered image into the cursor image
        // size and aspect ratio if it does not fit into 
        // the cursorSize area. Otherwise, draw the cursor 
        // into an image the size of the preferred cursor 
        // size.
        //
        Image bi = null;

        if (cursorSize.width < preferredSize.width 
            ||
            cursorSize.height < preferredSize.height) {
            float rw = cursorSize.width;
            float rh = (cursorSize.width * preferredSize.height) / (float)preferredSize.width;
            
            if (rh > cursorSize.height) {
                rw *= (cursorSize.height / rh);
                rh = cursorSize.height;
            }
            
            RenderedImage ri = f.createScaledRendering((int)Math.round(rw),
                                                       (int)Math.round(rh),
                                                       null);
            
            if (ri instanceof Image) {
                bi = (Image)ri;
            } else {
                bi = renderedImageToImage(ri);
            }

            // Apply the scale transform that is applied to the image
            x *= rw/preferredSize.width;
            y *= rh/preferredSize.height;

        } else {
            // Preferred size fits into ideal cursor size. No resize,
            // just draw cursor in 0, 0.
            BufferedImage tbi = new BufferedImage(cursorSize.width,
                                                  cursorSize.height,
                                                  BufferedImage.TYPE_INT_ARGB);
            RenderedImage ri = f.createScaledRendering(preferredSize.width,
                                                       preferredSize.height,
                                                       null);
            Graphics2D g = GraphicsUtil.createGraphics(tbi);
            GraphicsUtil.drawImage(g, ri);
            g.dispose();
            bi = tbi;
        }

        // Make sure the not spot does not fall out of the cursor area. If it
        // does, then clamp the coordinates to the image space.
        x = x < 0 ? 0 : x;
        y = y < 0 ? 0 : y;
        x = x > (cursorSize.width-1) ? cursorSize.width - 1 : x;
        y = y > (cursorSize.height-1) ? cursorSize.height - 1: y;

        //
        // The cursor image is now into the bi image
        //
        Cursor c = Toolkit.getDefaultToolkit()
            .createCustomCursor(bi, 
                                new Point((int)Math.round(x),
                                          (int)Math.round(y)),
                                purl.toString());

        cursorCache.putCursor(desc, c);
        return c;        
    }

    /**
     * Implementation helper: converts a RenderedImage to an Image
     */
    protected Image renderedImageToImage(RenderedImage ri) {
        int x = ri.getMinX();
        int y = ri.getMinY();
        SampleModel sm = ri.getSampleModel();
        ColorModel cm = ri.getColorModel();
        WritableRaster wr = Raster.createWritableRaster(sm, new Point(x,y));
        ri.copyData(wr);

        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /**
     * Simple inner class which holds the information describing
     * a cursor, i.e., the image it points to and the hot spot point
     * coordinates.
     */
    static class CursorDescriptor {
        ParsedURL purl;
        float x;
        float y;
        String desc;

        public CursorDescriptor(ParsedURL purl,
                                float x, float y) {
            if (purl == null) {
                throw new IllegalArgumentException();
            }

            this.purl = purl;
            this.x = x;
            this.y = y;

            // Desc is used for hascode as well as for toString()
            this.desc = this.getClass().getName() + 
                "\n\t:[" + this.purl + "]\n\t:[" + x + "]:[" + y + "]";
        }

        public boolean equals(Object obj) {
            if (obj == null 
                ||
                !(obj instanceof CursorDescriptor)) {
                return false;
            }

            CursorDescriptor desc = (CursorDescriptor)obj;
            boolean isEqual =  
                this.purl.equals(desc.purl)
                 &&
                 this.x == desc.x
                 &&
                 this.y == desc.y;
                 
            // System.out.println("isEqual : " + isEqual);
            // (new Exception()).printStackTrace();
            return isEqual;
        }

        public String toString() {
            return this.desc;
        }

        public int hashCode() {
            return desc.hashCode();
        }
    }

    /**
     * Simple extension of the SoftReferenceCache that 
     * offers typed interface (Kind of needed as SoftReferenceCache
     * mostly has protected methods).
     */
    static class CursorCache extends SoftReferenceCache {
        public CursorCache() {
        }

        public Cursor getCursor(CursorDescriptor desc) {
            return (Cursor)requestImpl(desc);
        }

        public void putCursor(CursorDescriptor desc, 
                              Cursor cursor) {
            putImpl(desc, cursor);
        }

        public void clearCursor(CursorDescriptor desc) {
            clearImpl(desc);
        }
    }



}
