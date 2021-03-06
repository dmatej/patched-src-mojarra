/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package javax.faces.event;


import javax.faces.component.UIComponent;


/**
 * <p><span class="changed_modified_2_0 changed_modified_2_2">A</span> listener interface for 
 * receiving {@link ActionEvent}s.  <span class="changed_added_2_0">An
 * implementation
 * of this interface must be thread-safe.</span>  A class that
 * is interested in receiving such events implements this interface, and then
 * registers itself with the source {@link UIComponent} of interest, by
 * calling <code>addActionListener()</code>.</p>
 */

public interface ActionListener extends FacesListener  {
    
    /**
     * <p class="changed_added_2_2">The presence of this component attribute
     * on an {@link javax.faces.component.ActionSource} component will cause
     * the default {@code ActionListener} to interpret the value of the 
     * attribute as the <em>toFlowDocumentId</em> value to pass to
     * {@link javax.faces.application.NavigationHandler#handleNavigation(javax.faces.context.FacesContext, java.lang.String, java.lang.String, java.lang.String)}.</p>
     * 
     */
    public static final String TO_FLOW_DOCUMENT_ID_ATTR_NAME = "to-flow-document-id";

    /**
     * <p>Invoked when the action described by the specified
     * {@link ActionEvent} occurs.</p>
     *
     * @param event The {@link ActionEvent} that has occurred
     *
     * @throws AbortProcessingException Signal the JavaServer Faces
     *  implementation that no further processing on the current event
     *  should be performed
     */
    public void processAction(ActionEvent event)
        throws AbortProcessingException;


}
