/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.apache.batik.dom.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.svg.SVGAnimatedBoolean;

/**
 * Provides support for the SVGExternalResourcesRequired interface.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class SVGExternalResourcesRequiredSupport implements SVGConstants {

    private static final String ATTR_NAME =
        SVG_EXTERNAL_RESOURCES_REQUIRED_ATTRIBUTE;

    /**
     * To implement {@link
     * org.w3c.dom.svg.SVGExternalResourcesRequired#getExternalResourcesRequired()}.
     */
    public static SVGAnimatedBoolean
        getExternalResourcesRequired(AbstractElement elt) {
        LiveAttributeValue lav;
        lav = elt.getLiveAttributeValue(null, ATTR_NAME);
        if (lav == null) {
            lav = new SVGOMAnimatedBoolean(elt, null, ATTR_NAME, false);
            elt.putLiveAttributeValue(null, ATTR_NAME, lav);
        }
        return (SVGAnimatedBoolean)lav;
    }
}
