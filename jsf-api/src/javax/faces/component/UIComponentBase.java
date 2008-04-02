/*
 * $Id: UIComponentBase.java,v 1.135 2006/07/12 22:51:38 rlubke Exp $
 */

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * https://javaserverfaces.dev.java.net/CDDL.html or
 * legal/CDDLv1.0.txt. 
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at legal/CDDLv1.0.txt.    
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * [Name of File] [ver.__] [Date]
 * 
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package javax.faces.component;


import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.render.Renderer;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p><strong>UIComponentBase</strong> is a convenience base class that
 * implements the default concrete behavior of all methods defined by
 * {@link UIComponent}.</p>
 *
 * <p>By default, this class defines <code>getRendersChildren()</code>
 * to find the renderer for this component and call its
 * <code>getRendersChildren()</code> method.  The default implementation
 * on the <code>Renderer</code> returns <code>false</code>.  As of
 * version 1.2 of the JavaServer Faces Specification, component authors
 * are encouraged to return <code>true</code> from this method and rely
 * on the implementation of {@link #encodeChildren} in this class and in
 * the Renderer ({@link Renderer#encodeChildren}).  Subclasses that wish
 * to manage the rendering of their children should override this method
 * to return <code>true</code> instead.</p>
 */

public abstract class UIComponentBase extends UIComponent {


    // -------------------------------------------------------------- Attributes

    private static Logger log = Logger.getLogger("javax.faces.component", 
            "javax.faces.LogStrings");


    /**
     * <p>Each entry is an map of <code>PropertyDescriptor</code>s describing
     * the properties of a concrete {@link UIComponent} implementation, keyed
     * by the corresponding <code>java.lang.Class</code>.</p>
     *
     * <p><strong>IMPLEMENTATION NOTE</strong> - This is implemented as a
     * <code>WeakHashMap</code> so that, even if this class is embedded in a
     * container's class loader that is a parent to webapp class loaders,
     * references to the classes will eventually expire.</p>
     */
    private static Map<Class<?>, Map<String, PropertyDescriptor>>
          descriptors =
          new WeakHashMap<Class<?>, Map<String, PropertyDescriptor>>();

    /**
     * Reference to the map of <code>PropertyDescriptor</code>s for this class
     * in the <code>descriptors<code> <code>Map<code>.
     */
    private Map<String,PropertyDescriptor> pdMap = null; 

    /**
     * <p>An empty argument list to be passed to reflection methods.</p>
     */
    private static Object empty[] = new Object[0];

    public UIComponentBase() {
        populateDescriptorsMapIfNecessary();
    }

    private void populateDescriptorsMapIfNecessary() {
        Class<?> clazz = this.getClass();
        pdMap = descriptors.get(clazz);
        if (null != pdMap) {
            return;
        }

        // load the property descriptors for this class.
        PropertyDescriptor pd[] = getPropertyDescriptors();
        if (pd != null) {
            pdMap = new WeakHashMap<String, PropertyDescriptor>(pd.length);
            for (PropertyDescriptor aPd : pd) {
                pdMap.put(aPd.getName(), aPd);
            }
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "fine.component.populating_descriptor_map",
                        new Object[]{clazz,
                                     Thread.currentThread().getName()});
            }

            // Check again
            Map<String, PropertyDescriptor> reCheckMap =
                  descriptors.get(clazz);
            if (null != reCheckMap) {
                return;
            }
            descriptors.put(clazz, pdMap);
        }


    }

    /**
     * <p>Return the <code>PropertyDescriptor</code> for the specified
     * property name for this {@link UIComponent}'s implementation class,
     * if any; otherwise, return <code>null</code>.</p>
     *
     * @param name Name of the property to return a descriptor for
     *
     * @throws FacesException if an introspection exception occurs
     */
    private PropertyDescriptor getPropertyDescriptor(String name) {
        if (pdMap != null) {
            return (pdMap.get(name));
        }
        return (null);
    }


    /**
     * <p>Return an array of <code>PropertyDescriptors</code> for this
     * {@link UIComponent}'s implementation class.  If no descriptors
     * can be identified, a zero-length array will be returned.</p>
     *
     * @throws FacesException if an introspection exception occurs
     */
    private PropertyDescriptor[] getPropertyDescriptors() {
        PropertyDescriptor[] pd = null;
        try {
            pd = Introspector.getBeanInfo(this.getClass()).
                getPropertyDescriptors();
        } catch (IntrospectionException e) {
            throw new FacesException(e);
        }
        return (pd);
    }


    /**
     * <p>The <code>Map</code> containing our attributes, keyed by
     * attribute name.</p>
     */
    private AttributesMap attributes = null;


    public Map<String, Object> getAttributes() {

        if (attributes == null) {
            attributes = new AttributesMap();
        }
        return (attributes);

    }


    // ---------------------------------------------------------------- Bindings


    /**
     * {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @deprecated This has been replaced by {@link #getValueExpression}.
     */ 
    public ValueBinding getValueBinding(String name) {

	if (name == null) {
	    throw new NullPointerException();
	}
	ValueBinding result = null;
	ValueExpression ve = null;

	if (null != (ve = getValueExpression(name))) {
	    // if the ValueExpression is an instance of our private
	    // wrapper class.
	    if (ve.getClass() == ValueExpressionValueBindingAdapter.class) {
		result = ((ValueExpressionValueBindingAdapter)ve).getWrapped();
	    }
	    else {
		// otherwise, this is a real ValueExpression.  Wrap it
		// in a ValueBinding.
		result = new ValueBindingValueExpressionAdapter(ve);
	    }
	}
	return result;
    }


    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @deprecated This has been replaced by {@link #setValueExpression}.
     */ 
    public void setValueBinding(String name, ValueBinding binding) {
	if (name == null) {
	    throw new NullPointerException();
	} 
	if (binding != null) {
	    ValueExpressionValueBindingAdapter adapter = 
		new ValueExpressionValueBindingAdapter(binding);
	    setValueExpression(name, adapter);
	} else {
	    setValueExpression(name, null);
	}

    }

    /**
     * {@inheritDoc}
     * @since 1.2
     * @throws NullPointerException {@inheritDoc}
     */ 
    public ValueExpression getValueExpression(String name) {
        return super.getValueExpression(name);
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */ 
    public void setValueExpression(String name, ValueExpression binding) {
        super.setValueExpression(name, binding);
    }
    
    // -------------------------------------------------------------- Properties


    /**
     * <p>The assigned client identifier for this component.</p>
     */
    private String clientId = null;

    
    /**
     * @throws NullPointerException {@inheritDoc}
     */ 
    public String getClientId(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        // if the clientId is not yet set
        if (this.clientId == null) {
            UIComponent parent = this.getNamingContainer();
            String parentId = null;
            
            // give the parent the opportunity to first
            // grab a unique clientId
            if (parent != null) {
                parentId = parent.getContainerClientId(context);
            }
            
            // now resolve our own client id
            this.clientId = this.id;
            if (this.clientId == null) {
                this.clientId = context.getViewRoot().createUniqueId();
            }
            if (parentId != null) {
                this.clientId = parentId + NamingContainer.SEPARATOR_CHAR + this.clientId;
            }
            
            // allow the renderer to convert the clientId
            Renderer renderer = this.getRenderer(context);
            if (renderer != null) {
                this.clientId = renderer.convertClientId(context, this.clientId);
            }
        }
        return this.clientId;
    }
    
    /**
     * <p>Private utilitity method for finding this
     * <code>UIComponent</code>'s parent <code>NamingContainer</code>.
     * This method may return <code>null</code> if there is not a
     * parent <code>NamingContainer</code></p>
     * 
     * @return the parent <code>NamingContainer</code>
     */
    private UIComponent getNamingContainer() {
        UIComponent namingContainer = this.getParent();
        while (namingContainer != null) {
            if (namingContainer instanceof NamingContainer) {
                return namingContainer;
            }
            namingContainer = namingContainer.getParent();
        }
        return null;
    }



    /**
     * <p>The component identifier for this component.</p>
     */
    private String id = null;


    public String getId() {

	    return (id);

    }


    /**
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}    
     */ 
    public void setId(String id) {
        
        validateId(id);
        this.id = id;
        this.clientId = null; // Erase any cached value

    }


    /**
     * <p>The parent component for this component.</p>
     */
    private UIComponent parent = null;


    public UIComponent getParent() {
        return (this.parent);
    }


    public void setParent(UIComponent parent) {
        this.parent = parent;
    }


    /**
     * <p>The "should this component be rendered" flag.</p>
     */
    private boolean rendered = true;
    private boolean renderedSet = false;

    public boolean isRendered() {

	if (renderedSet) {
	    return (rendered);
	}
	ValueExpression ve = getValueExpression("rendered");
	if (ve != null) {
	    boolean result = false;
	    try {
		result = !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
	    }
	    catch (ELException e) {
		throw new FacesException(e);
	    }
	    return result;
	} else {
	    return (this.rendered);
	}

    }
    

    public void setRendered(boolean rendered) {

        this.rendered = rendered;
	this.renderedSet = true;

    }


    /**
     * <p>The renderer type for this component.</p>
     */
    private String rendererType = null;


    public String getRendererType() {

	if (this.rendererType != null) {
	    return (this.rendererType);
	}
	ValueExpression ve = getValueExpression("rendererType");
	if (ve != null) {
	    String result = null;
	    try {
		result = (String)ve.getValue(getFacesContext().getELContext());
	    }
	    catch (ELException e) {
		throw new FacesException(e);
	    }
	    return result;
	} else {
	    return (null);
	}

    }


    public void setRendererType(String rendererType) {

        this.rendererType = rendererType;

    }


    public boolean getRendersChildren() {
        boolean result = false;

        Renderer renderer = null;
        if (getRendererType() != null) {
            if (null != 
                (renderer = getRenderer(getFacesContext()))) {
                result = renderer.getRendersChildren();
            }
        }
        return result;

    }
    
    // ------------------------------------------------- Tree Management Methods


    /*
     * <p>The <code>List</code> containing our child components.</p>
     */
    private List<UIComponent> children = null;


    public List<UIComponent> getChildren() {

        if (children == null) {
            children = new ChildrenList();
        }
        return (children);

    }


    // Do not allocate the children List to answer this question
    public int getChildCount() {

        if (children != null) {
            return (children.size());
        } else {
            return (0);
        }

    }


    /**
     * <p>If the specified {@link UIComponent} has a non-null parent,
     * remove it as a child or facet (as appropriate) of that parent.
     * As a result, the <code>parent</code> property will always be
     * <code>null</code> when this method returns.</p>
     *
     * @param component {@link UIComponent} to have any parent erased
     */
    private void eraseParent(UIComponent component) {

        UIComponent parent = component.getParent();
        if (parent == null) {
            return;
        }
        if (parent.getChildCount() > 0) {
            List children = parent.getChildren();
            int index = children.indexOf(component);
            if (index >= 0) {
                children.remove(index);
                return;
            }
        }
        if (parent.getFacetCount() > 0) {
            Map facets = parent.getFacets();
            Iterator entries = facets.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                if (entry.getValue() == component) {
                    entries.remove();
                    return;
                }
            }
        }

        // Throw an exception for the "cannot happen" case
        throw new IllegalStateException("Parent was not null, " +
                                        "but this component not related");

    }

    /**
     * <p>Throw <code>IllegalArgumentException</code> if the specified
     * component identifier is non-<code>null</code> and not
     * syntactically valid.  </p>
     *
     * @param id The component identifier to test
     */
    private void validateId(String id) {

        if (id == null) {
            return;
        }
        int n = id.length();
        if (n < 1) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < n; i++) {
            char c = id.charAt(i);
            if (i == 0) {
                if (!Character.isLetter(c) && (c != '_')) {
                    throw new IllegalArgumentException(id);
                }
            } else {
                if (!Character.isLetter(c) &&
                    !Character.isDigit(c) &&
                    (c != '-') && (c != '_')) {
                    throw new IllegalArgumentException(id);
                }
            }
        }

    }


    private static final String SEPARATOR_STRING =
        "" + NamingContainer.SEPARATOR_CHAR;

    /**
     * @throws NullPointerException {@inheritDoc}
     */ 
    public UIComponent findComponent(String expr) {

        if (expr == null) {
            throw new NullPointerException();
        }

        // Identify the base component from which we will perform our search
        UIComponent base = this;
        if (expr.charAt(0) == NamingContainer.SEPARATOR_CHAR) {
            // Absolute searches start at the root of the tree
            while (base.getParent() != null) {
                base = base.getParent();
            }
            // Treat remainder of the expression as relative
            expr = expr.substring(1);
        } else {
            // Relative expressions start at the closest NamingContainer or root
            while (base.getParent() != null) {
                if (base instanceof NamingContainer) {
                    break;
                }
                base = base.getParent();
            }
        }

        // Evaluate the search expression (now guaranteed to be relative)        
        UIComponent result = null;
        String[] segments = expr.split(SEPARATOR_STRING);
        for (int i = 0, length = (segments.length - 1);
             i < segments.length;
             i++, length--) {
            result = findComponent(base, segments[i], (length == 0));
            // the first element of the expression may match base.id 
            // (vs. a child if of base)
            if (i == 0 && result == null && segments[i].equals(base.getId())) {
                result = base;
            }          
            if (result == null && length > 0) {
                throw new IllegalArgumentException(segments[i]);
            }
            base = result;
        }

        // Return the final result of our search
        return (result);

    }

    
    /**
     * <p>Return the {@link UIComponent} (if any) with the specified
     * <code>id</code>, searching recursively starting at the specified
     * <code>base</code>, and examining the base component itself, followed
     * by examining all the base component's facets and children (unless
     * the base component is a {@link NamingContainer}, in which case the
     * recursive scan is skipped.</p>
     *
     * @param base Base {@link UIComponent} from which to search
     * @param id Component identifier to be matched
     */
    private UIComponent findComponent(UIComponent base,
                                      String id, 
                                      boolean checkId) {
        
        // Search through our facets and children       
        UIComponent result = null;
        for (Iterator i = base.getFacetsAndChildren(); i.hasNext(); ) {
            UIComponent kid = (UIComponent) i.next(); 
            if (!(kid instanceof NamingContainer)) {
                if (checkId && id.equals(kid.getId())) {
                    result = kid;
                    break;
                }
                result = findComponent(kid, id, checkId);
                if (result != null) {
                    break;
                }
            } else if (id.equals(kid.getId())) {
                result = kid;
                break;
            }
        }                
        
        return (result);

    }

    /**
     * {@inheritDoc}
     * @since 1.2
     * @throws NullPointerException {@inheritDoc}
     * @throws FacesException {@inheritDoc}
     *
     */ 
    public boolean invokeOnComponent(FacesContext context, String clientId, 
				     ContextCallback callback) 
	throws FacesException {
        return super.invokeOnComponent(context, clientId, callback);
    }


    // ------------------------------------------------ Facet Management Methods


    /*
     * <p>The <code>Map</code> containing our related facet components.</p>
     */
    private Map<String, UIComponent> facets = null;


    public Map<String, UIComponent> getFacets() {

        if (facets == null) {
            facets = new FacetsMap();
        }
        return (facets);

    }
    
    // Do not allocate the children List to answer this question
    public int getFacetCount() {

        if (facets != null) {
            return (facets.size());
        } else {
            return (0);
        }

    }


    // Do not allocate the facets Map to answer this question
    public UIComponent getFacet(String name) {

        if (facets != null) {
            return ((UIComponent) facets.get(name));
        } else {
            return (null);
        }

    }


    public Iterator<UIComponent> getFacetsAndChildren() {
                
        Iterator<UIComponent> result = null;
        int childCount = this.getChildCount(), 
                facetCount = this.getFacetCount();
        // If there are neither facets nor children
        if (0 == childCount && 0 == facetCount) {
            result = EMPTY_ITERATOR;
        }
        // If there are only facets and no children
        else if (0 == childCount) {
            Collection<UIComponent> unmodifiable = 
              Collections.unmodifiableCollection(getFacets().values());
            result = unmodifiable.iterator();
        }
        // If there are only children and no facets
        else if (0 == facetCount) {
            List<UIComponent> unmodifiable = 
              Collections.unmodifiableList(getChildren());
            result = unmodifiable.iterator();
        }
        // If there are both children and facets
        else {
            result = new FacetsAndChildrenIterator(this);
        }
        return result;
    }


    // -------------------------------------------- Lifecycle Processing Methods

    /**
     * @throws AbortProcessingException {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}  
     */ 
    public void broadcast(FacesEvent event)
        throws AbortProcessingException {

        if (event == null) {
            throw new NullPointerException();
        }
        if (listeners == null) {
            return;
        }

        Iterator<FacesListener> iter = listeners.iterator();
        while (iter.hasNext()) {
            FacesListener listener = iter.next();
            if (event.isAppropriateListener(listener)) {
                event.processListener(listener);
            }
        }
    }


    /**
     * @throws NullPointerException {@inheritDoc}     
     */ 
    public void decode(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }
        String rendererType = getRendererType();
        if (rendererType != null) {
            getRenderer(context).decode(context, this);
        }
    }


    /**
     * @throws NullPointerException {@inheritDoc}   
     */ 
    public void encodeBegin(FacesContext context) throws IOException {

        if (context == null) {
            throw new NullPointerException();
        }
        if (!isRendered()) {
            return;
        }
        String rendererType = getRendererType();
        if (rendererType != null) {
            getRenderer(context).encodeBegin(context, this);
        }

    }

    /**
     * @throws NullPointerException {@inheritDoc}     
     */ 
    public void encodeChildren(FacesContext context) throws IOException {

        if (context == null) {
            throw new NullPointerException();
        }
        if (!isRendered()) {
            return;
        }
        String rendererType = getRendererType();
        if (rendererType != null) {
            getRenderer(context).encodeChildren(context, this);
        }

    }


    /**
     * @throws IOException {@inheritDoc}   
     * @throws NullPointerException {@inheritDoc}   
     */ 
    public void encodeEnd(FacesContext context) throws IOException {

        if (context == null) {
            throw new NullPointerException();
        }
        if (!isRendered()) {
            return;
        }
        String rendererType = getRendererType();
        if (rendererType != null) {
            getRenderer(context).encodeEnd(context, this);
        }

    }

    // -------------------------------------------------- Event Listener Methods


    /**
     * <p>Our {@link javax.faces.event.FacesListener}s.  This data
     * structure is lazily instantiated as necessary.</p>
     */
    private List<FacesListener> listeners;


    /**
     * <p>Add the specified {@link FacesListener} to the set of listeners
     * registered to receive event notifications from this {@link UIComponent}.
     * It is expected that {@link UIComponent} classes acting as event sources
     * will have corresponding typesafe APIs for registering listeners of the
     * required type, and the implementation of those registration methods
     * will delegate to this method.  For example:</p>
     * <pre>
     * public class FooEvent extends FacesEvent {
     *   ...
     *   protected boolean isAppropriateListener(FacesListener listener) {
     *     return (listener instanceof FooListener);
     *   }
     *   protected void processListener(FacesListener listener) {
     *     ((FooListener) listener).processFoo(this);
     *   }
     *   ...
     * }
     *
     * public interface FooListener extends FacesListener {
     *   public void processFoo(FooEvent event);
     * }
     *
     * public class FooComponent extends UIComponentBase {
     *   ...
     *   public void addFooListener(FooListener listener) {
     *     addFacesListener(listener);
     *   }
     *   public void removeFooListener(FooListener listener) {
     *     removeFacesListener(listener);
     *   }
     *   ...
     * }
     * </pre>
     *
     * @param listener The {@link FacesListener} to be registered
     *
     * @throws NullPointerException if <code>listener</code>
     *  is <code>null</code>
     */
    protected void addFacesListener(FacesListener listener) {

        if (listener == null) {
            throw new NullPointerException();
        }
        if (listeners == null) {
            listeners = new ArrayList<FacesListener>();
        }
        listeners.add(listener);

    }


    /**
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}  
     */ 
    protected FacesListener[] getFacesListeners(Class clazz) {
        if (clazz == null) {
            throw new NullPointerException();
        }
        if (!FacesListener.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException();
        }
        if (listeners == null) {
            return ((FacesListener[]) 
                java.lang.reflect.Array.newInstance(clazz, 0));
        }

        List results = new ArrayList();
	Iterator<FacesListener> items = listeners.iterator();
	while (items.hasNext()) {
	    FacesListener item = items.next();
	    if (clazz.isAssignableFrom(item.getClass())) {
		results.add(item);
	    }
	}
	
        return ((FacesListener[]) results.toArray
                ((Object []) java.lang.reflect.Array.newInstance(clazz, 
								 results.size())));

    }


    /**
     * <p>Remove the specified {@link FacesListener} from the set of listeners
     * registered to receive event notifications from this {@link UIComponent}.
     *
     * @param listener The {@link FacesListener} to be deregistered
     *
     * @throws NullPointerException if <code>listener</code>
     *  is <code>null</code>
     */
    protected void removeFacesListener(FacesListener listener) {

        if (listener == null) {
            throw new NullPointerException();
        }
        if (listeners == null) {
            return;
        }
	listeners.remove(listener);
    }

    /**
     * @throws IllegalStateException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}  
     */ 
    public void queueEvent(FacesEvent event) {

        if (event == null) {
            throw new NullPointerException();
        }
        UIComponent parent = getParent();
        if (parent == null) {
            throw new IllegalStateException();
        } else {
            parent.queueEvent(event);
        }

    }


    // ------------------------------------------------ Lifecycle Phase Handlers


    /**
     * @throws NullPointerException {@inheritDoc}     
     */ 
    public void processDecodes(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        // Skip processing if our rendered flag is false
        if (!isRendered()) {
            return;
        }

        // Process all facets and children of this component
        Iterator kids = getFacetsAndChildren();
        while (kids.hasNext()) {
            UIComponent kid = (UIComponent) kids.next();
            kid.processDecodes(context);
        }

        // Process this component itself
        try {
            decode(context);
        } catch (RuntimeException e) {
            context.renderResponse();
            throw e;
        }

    }


    /**
     * @throws NullPointerException {@inheritDoc}    
     */ 
    public void processValidators(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        // Skip processing if our rendered flag is false
        if (!isRendered()) {
            return;
        }

        // Process all the facets and children of this component
        Iterator kids = getFacetsAndChildren();
        while (kids.hasNext()) {
            UIComponent kid = (UIComponent) kids.next();
            kid.processValidators(context);
        }
    }


    /**
     * @throws NullPointerException {@inheritDoc}     
     */ 
    public void processUpdates(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        // Skip processing if our rendered flag is false
        if (!isRendered()) {
            return;
        }

        // Process all facets and children of this component
        Iterator kids = getFacetsAndChildren();
        while (kids.hasNext()) {
            UIComponent kid = (UIComponent) kids.next();
            kid.processUpdates(context);
        }
    }

    private static final int MY_STATE = 0;
    private static final int CHILD_STATE = 1;

    /**
     * @throws NullPointerException {@inheritDoc}         
     */ 
    public Object processSaveState(FacesContext context) {
        
        if (context == null) {
            throw new NullPointerException();
        }
        if (this.isTransient()) {
            return null;
        }
        Object [] stateStruct = new Object[2];
        Object [] childState = EMPTY_ARRAY;
        
        // Process this component itself
        stateStruct[MY_STATE] = saveState(context);
        
        // determine if we have any children to store
        int count = this.getChildCount() + this.getFacetCount();
        if (count > 0) {
            
            // this arraylist will store state
            List<Object> stateList = new ArrayList<Object>(count);
            
            // if we have children, add them to the stateList
            if (this.getChildCount() > 0) {
                Iterator kids = getChildren().iterator();
                UIComponent kid;
                while (kids.hasNext()) {
                    kid = (UIComponent) kids.next();
                    if (!kid.isTransient()) {
                        stateList.add(kid.processSaveState(context));
                    }
                }
            }
            
            // if we have facets, add them to the stateList
            if (this.getFacetCount() > 0) {
                Iterator myFacets = getFacets().entrySet().iterator();
                UIComponent facet = null;
                Object facetState = null;
                Object[] facetSaveState = null;
                Map.Entry entry = null;
                while (myFacets.hasNext()) {
                    entry = (Map.Entry) myFacets.next();
                    facet = (UIComponent) entry.getValue();
                    if (!facet.isTransient()) {
                        facetState = facet.processSaveState(context);
                        facetSaveState = new Object[2];
                        facetSaveState[0] = entry.getKey();
                        facetSaveState[1] = facetState;
                        stateList.add(facetSaveState);
                    }
                }
            }
            
            // finally, capture the stateList and replace the original,
            // empty Object array
            childState = stateList.toArray();
        }
        
        stateStruct[CHILD_STATE] = childState;
        return stateStruct;
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public void processRestoreState(FacesContext context,
                                    Object state) {
        if (context == null) {
            throw new NullPointerException();
        }
        
        Object [] stateStruct = (Object []) state;
        Object [] childState = (Object []) stateStruct[CHILD_STATE];
        
        // Process this component itself
        restoreState(context, stateStruct[MY_STATE]);
        
        int i = 0;
        
        // Process all the children of this component
        if (this.getChildCount() > 0) {
            Iterator kids = getChildren().iterator();
            while (kids.hasNext()) {
                UIComponent kid = (UIComponent) kids.next();
                if (kid.isTransient()) {
                    continue;
                }
                Object currentState = childState[i++];
                if (currentState == null) {
                    continue;
                }
                kid.processRestoreState(context, currentState);
            }
        }
        
        // process all of the facets of this component
        if (this.getFacetCount() > 0) {
            int facetsSize = getFacets().size();
            int j = 0;
            Object[] facetSaveState = null;
            String facetName = null;
            UIComponent facet = null;
            Object facetState = null;
            while (j < facetsSize) {
                if (null != (facetSaveState = (Object[])childState[i++])) {
                    facetName = (String) facetSaveState[0];
                    facetState = facetSaveState[1];
                    facet = (UIComponent) getFacets().get(facetName);
                    facet.processRestoreState(context, facetState);
                }
                ++j;
            }
        }
    }
    
    // ------------------------------------------------------- Protected Methods


    protected FacesContext getFacesContext() {

	// PENDING(edburns): we can't use the cache ivar because we
	// don't always know when to clear it.  For example, in the
	// "save state in server" case, the UIComponent instances stick
	// around between requests, yielding stale facesContext
	// references.  If there was some way to clear the facesContext
	// cache ivar for each node in the tree *after* the
	// render-response phase, then we could keep a cache ivar.  As
	// it is now, we must always use the Thread Local Storage
	// solution.

	return FacesContext.getCurrentInstance();

    }


    protected Renderer getRenderer(FacesContext context) {

        String rendererType = getRendererType();
        Renderer result = null;
        if (rendererType != null) {
            result = context.getRenderKit().getRenderer(getFamily(),
                                                        rendererType);
            if (null == result) {
                if (log.isLoggable(Level.FINE)) {
                    // PENDING(edburns): I18N
                    log.fine("Can't get Renderer for type " + rendererType);
                }
            }
        } else {
            if (log.isLoggable(Level.FINE)) {
                String id = this.getId();
                id = (null != id) ? id : this.getClass().getName();
                // PENDING(edburns): I18N
                log.fine("No renderer-type for component " + id);
            }
        }
        return result;
    }


    // ----------------------------------------------------- StateHolder Methods
    private Object[] values;

    public Object saveState(FacesContext context) {

        if (values == null) {
             values = new Object[8];
        }
        
        if (attributes != null) {
            Map backing = attributes.getBackingAttributes();
            if (backing != null && !backing.isEmpty()) {
                values[0] = backing;
            }
        }
        values[1] = saveBindingsState(context);
        values[2] = clientId;
        values[3] = id;
        values[4] = rendered ? Boolean.TRUE : Boolean.FALSE;
        values[5] = renderedSet ? Boolean.TRUE : Boolean.FALSE;
        values[6] = rendererType;
        values[7] = saveAttachedState(context, listeners);
        assert(!transientFlag);
        
        return (values);
    }


    public void restoreState(FacesContext context, Object state) {

        values = (Object[]) state;
        // we need to get the map that knows how to handle attribute/property 
        // transparency before we restore its values.        
        if ( values[0] != null ) {
            attributes = new AttributesMap((Map) values[0]);
        }
        bindings = restoreBindingsState(context, values[1]);
        clientId = (String) values[2];
        id = (String) values[3];
        rendered = ((Boolean) values[4]).booleanValue();
        renderedSet = ((Boolean) values[5]).booleanValue();
        rendererType = (String) values[6];
        List restoredListeners = null;
        if (null != (restoredListeners = (List)
                     restoreAttachedState(context, values[7]))) {
            // if there were some listeners registered prior to this
            // method being invoked, merge them with the list to be
            // restored.
            if (null != listeners) {
		listeners.addAll(restoredListeners);
            }
            else {
                listeners = restoredListeners;
            }
        }
    }


    /**
     * <p>Flag indicating a desire to now participate in state saving.</p>
     */
    private boolean transientFlag = false;


    public boolean isTransient() {

        return (this.transientFlag);

    }


    public void setTransient(boolean transientFlag) {

        this.transientFlag = transientFlag;

    }

    // -------------------------------------- Helper methods for state saving

    // --------- methods used by UIComponents to save their attached Objects.

    /**
     *
     * <p>This method is called by {@link UIComponent} subclasses that
     * want to save one or more attached objects.  It is a convenience
     * method that does the work of saving attached objects that may or
     * may not implement the {@link StateHolder} interface.  Using this
     * method implies the use of {@link #restoreAttachedState} to restore
     * the attached objects.</p>
     *
     * <p>This method supports saving  attached objects of the following
     * type: <code>Object</code>s,
     * <code>null</code> values, and <code>Lists</code> of these
     * objects.  If any contained objects are not <code>Lists</code>
     * and do not implement {@link StateHolder}, they must have
     * zero-argument public constructors.  The exact structure of the
     * returned object is undefined and opaque, but will be serializable.
     * </p>
     *
     * @param context the {@link FacesContext} for this request.
     *
     * @param attachedObject the object, which may be a
     * <code>List</code> instance, or an Object.  The
     * <code>attachedObject</code> (or the elements that comprise
     * <code>attachedObject</code> may implement {@link StateHolder}.
     *
     * @throws NullPointerException if the context argument is null.
     *
     */

    public static Object saveAttachedState(FacesContext context,
                                           Object attachedObject) {
        if (null == context) {
            throw new NullPointerException();
        }
        if (null == attachedObject) {
            return null;
        }
        Object result = null;
        List
            attachedList = null,
            resultList = null;
        Iterator listIter = null;

        if (attachedObject instanceof List) {
            attachedList = (List) attachedObject;
            resultList = new ArrayList(attachedList.size());
            listIter = attachedList.iterator();
	    Object cur = null;
            while (listIter.hasNext()) {
		if (null != (cur = listIter.next())) {
		    resultList.add(new StateHolderSaver(context, cur));
		}
            }
            result = resultList;
        }
        else {
            result = new StateHolderSaver(context, attachedObject);
        }

        return result;
    }
    
    /**
     *
     * <p>This method is called by {@link UIComponent} subclasses that
     * need to restore the objects they saved using {@link
     * #saveAttachedState}.  This method is tightly coupled with {@link
     * #saveAttachedState}.</p>
     *
     * <p>This method supports restoring all attached objects types
     * supported by {@link #saveAttachedState}.</p>
     *
     * @param context the {@link FacesContext} for this request
     *
     * @param stateObj the opaque object returned from {@link
     * #saveAttachedState}
     *
     * @throws NullPointerException if context is null.
     *
     * @throws IllegalStateException if the object is not 
     *   previously returned by {@link #saveAttachedState}.
     *
     */

    public static Object restoreAttachedState(FacesContext context,
                                              Object stateObj)
    throws IllegalStateException {
        if (null == context) {
            throw new NullPointerException();
        }
        if (null == stateObj) {
            return null;
        }
        Object result;       

        if (stateObj instanceof List) {
            List stateList = (List) stateObj;
            List<Object> retList = new ArrayList<Object>(stateList.size());
            for (Object item : stateList) {
                try {
                    retList.add(((StateHolderSaver) item).restore(context));    
                } catch (ClassCastException cce) {
                    throw new IllegalStateException("Unknown object type");
                }  
            }
            result = retList;
        } else if (stateObj instanceof StateHolderSaver) {
            StateHolderSaver saver = (StateHolderSaver) stateObj;
            result = saver.restore(context);
        } else {
            throw new IllegalStateException("Unknown object type");
        }
        return result;
    }

    private Map restoreBindingsState(FacesContext context, Object state) {

	if (state == null) {
	    return (null);
	}
	Object values[] = (Object[]) state;
	String names[] = (String[]) values[0];
	Object states[] = (Object[]) values[1];
	Map<String,Object> bindings = new HashMap<String,Object>(names.length);
	for (int i = 0; i < names.length; i++) {
	    bindings.put(names[i],
			 restoreAttachedState(context, states[i]));
	}
	return (bindings);

    }


    private Object saveBindingsState(FacesContext context) {
        
        if (bindings == null) { 
            return (null); 
        } 
         
        Object values[] = new Object[2]; 
        values[0] = bindings.keySet().toArray(new String[bindings.size()]); 
         
        Object[] bindingValues = bindings.values().toArray(); 
        for (int i = 0; i < bindingValues.length; i++) { 
            bindingValues[i] = saveAttachedState(context, bindingValues[i]); 
        } 
         
        values[1] = bindingValues; 
                
        return (values); 

    }


    // --------------------------------------------------------- Private Classes

    // For state saving
    private final static Object[] EMPTY_ARRAY = new Object[0];
    
    // Empty iterator for short circuiting operations
    private final static Iterator EMPTY_ITERATOR = new Iterator() {
    
        public void remove() {
            throw new UnsupportedOperationException();
        }
    
        public Object next() {
            throw new NoSuchElementException("Empty Iterator");
        }
    
        public boolean hasNext() {
            return false;
        }
    };

    // Private implementation of Map that supports the functionality
    // required by UIComponent.getFacets()
    // HISTORY:
    //   Versions 1.333 and older used inheritence to provide the
    //     basic map functionality.  This was wasteful since a 
    //     component could be completely configured via ValueExpressions
    //     or (Bindings) which means an empty Map would always be
    //     present when it wasn't needed.  By using composition,
    //     we control if and when the Map is instantiated thereby
    //     reducing uneeded object allocation.  This change also
    //     has a nice side effect in state saving since we no
    //     longer need to duplicate the map, we just provide the
    //     private 'attributes' map directly to the state saving process.
    private class AttributesMap implements Map, Serializable {
        
        private Map attributes;
        
        // -------------------------------------------------------- Constructors
        
        private AttributesMap() { }
        
        private AttributesMap(Map<String,Object> attributes) {
            this.attributes = attributes;            
        }

        public boolean containsKey(Object keyObj) {
            String key = (String) keyObj;
            PropertyDescriptor pd =
                getPropertyDescriptor(key);
            if (pd == null) {
                if (attributes != null) {
                    return attributes.containsKey(key);
                } else {
                    return (false);
                }                
            } else {
                return (false);
            }
        }

        public Object get(Object keyObj) {
            String key = (String) keyObj;
            if (key == null) {
                throw new NullPointerException();
            }
            PropertyDescriptor pd =
                getPropertyDescriptor(key);
            if (pd != null) {
                try {
                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        return (readMethod.invoke
                                (UIComponentBase.this, empty));
                    } else {
                        throw new IllegalArgumentException(key);
                    }
                } catch (IllegalAccessException e) {
                    throw new FacesException(e);
                } catch (InvocationTargetException e) {
                    throw new FacesException
                        (e.getTargetException());
                }
            } else if (attributes != null) {
                if (attributes.containsKey(key)) {
                    return (attributes.get(key));
                }
            }
            ValueExpression ve = getValueExpression(key);
            if (ve != null) {
                Object result = null;
                try {
                    result = ve.getValue(getFacesContext().getELContext());
                    return result;
                }
                catch (ELException e) {
                    throw new FacesException(e);
                }
            }
            return (null);
        }

        public Object put(Object keyValue, Object value) {
            if (keyValue == null) {
                throw new NullPointerException();
            }
            
            if (!(keyValue instanceof String)) {
                // PENDING i18n
                throw new ClassCastException("Key must be a String");
            }

            String key = keyValue.toString();
            PropertyDescriptor pd =
                getPropertyDescriptor(key);
            if (pd != null) {
                try {
                    Object result = null;
                    Method readMethod = pd.getReadMethod();
                    if (readMethod != null) {
                        result = readMethod.invoke
                            (UIComponentBase.this, empty);
                    }
                    Method writeMethod = pd.getWriteMethod();
                    if (writeMethod != null) {
                        writeMethod.invoke
                            (UIComponentBase.this,
                             new Object[] { value });
                    } else {
                        throw new IllegalArgumentException();
                    }
                    return (result);
                } catch (IllegalAccessException e) {
                    throw new FacesException(e);
                } catch (InvocationTargetException e) {
                    throw new FacesException
                        (e.getTargetException());
                }
            } else {
                if (value == null) {
                    throw new NullPointerException();
                }
                if (attributes == null) {
                    initMap();
                }
                return (attributes.put(key, value));
            }
        }

        public void putAll(Map map) {
            if (map == null) {
                throw new NullPointerException();
            }
            
            if (attributes == null) {
                initMap();
            }
            attributes.putAll(map);
        }

        public Object remove(Object keyObj) {
            String key = (String) keyObj;
            if (key == null) {
                throw new NullPointerException();
            }
            PropertyDescriptor pd =
                getPropertyDescriptor(key);
            if (pd != null) {
                throw new IllegalArgumentException(key);
            } else {
                if (attributes != null) {
                    return (attributes.remove(key));
                } else {
                    return null;
                }
            }
        }


        public int size() {
            return (attributes != null ? attributes.size() : 0);            
        }

        public boolean isEmpty() {
            return (attributes == null || attributes.isEmpty());
        }

        public boolean containsValue(java.lang.Object value) {
            return (attributes != null && attributes.containsValue(value));
        }

        public void clear() {
            if (attributes != null) {
                attributes.clear();
            }
        }

        public Set keySet() {
            return (attributes != null
                    ? attributes.keySet()
                    : Collections.EMPTY_SET);
        }

        public Collection values() {
            return (attributes != null
                    ? attributes.values()
                    : Collections.EMPTY_LIST);
        }

        public Set entrySet() {
            return (attributes != null
                    ? attributes.entrySet()
                    : Collections.EMPTY_SET);
        }
        
        Map getBackingAttributes() {
            return attributes;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof Map)) {
                return false;
            }
            Map t = (Map) o;
            if (t.size() != size()) {
                return false;
            }

            try {                   
                for (Object e : entrySet()) {
                    Entry entry = (Entry) e;
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (value == null) {
                        if (!(t.get(key) == null && t.containsKey(key))) {
                            return false;
                        }
                    } else {
                        if (!value.equals(t.get(key))) {
                            return false;
                        }
                    }
                }
            } catch (ClassCastException unused) {
                return false;
            } catch (NullPointerException unused) {
                return false;
            }

            return true;
        }


        public int hashCode() {
            int h = 0;
            for (Object o : entrySet()) {
                h += o.hashCode();
            }
            return h;
        }
        
        private void initMap() {
            attributes = new HashMap(8);
        }
    }


    // Private implementation of List that supports the functionality
    // required by UIComponent.getChildren()
    private class ChildrenList extends ArrayList<UIComponent> {

        public void add(int index, UIComponent element) {
            if (element == null) {
                throw new NullPointerException();
            } else if ((index < 0) || (index > size())) {
                throw new IndexOutOfBoundsException();
            } else {
                eraseParent(element);
                element.setParent(UIComponentBase.this);
                super.add(index, element);
            }
        }

        public boolean add(UIComponent element) {
            if (element == null) {
                throw new NullPointerException();
            } else {
                eraseParent(element);
                element.setParent(UIComponentBase.this);
                return (super.add(element));
            }
        }

        public boolean addAll(Collection<? extends UIComponent> collection) {
            Iterator<UIComponent> elements =
                (new ArrayList<UIComponent>(collection)).iterator();
            boolean changed = false;
            while (elements.hasNext()) {
                UIComponent element = elements.next();
                if (element == null) {
                    throw new NullPointerException();
                } else {
                    add(element);
                    changed = true;
                }
            }
            return (changed);
        }

        public boolean addAll(int index, Collection<? extends UIComponent> collection) {
            Iterator<UIComponent> elements =
             (new ArrayList<UIComponent>(collection)).iterator();
            boolean changed = false;
            while (elements.hasNext()) {
                UIComponent element = elements.next();
                if (element == null) {
                    throw new NullPointerException();
                } else {
                    add(index++, element);
                    changed = true;
                }
            }
            return (changed);
        }

        public void clear() {
            int n = size();
            if (n < 1) {
                return;
            }
            for (int i = 0; i < n; i++) {
                UIComponent child = get(i);
                child.setParent(null);
            }
            super.clear();
        }

        public Iterator<UIComponent> iterator() {
            return (new ChildrenListIterator(this));
        }

        public ListIterator<UIComponent> listIterator() {
            return (new ChildrenListIterator(this));
        }

        public ListIterator<UIComponent> listIterator(int index) {
            return (new ChildrenListIterator(this, index));
        }

        public UIComponent remove(int index) {
            UIComponent child = get(index);
            super.remove(index);
            child.setParent(null);
            return (child);
        }

        public boolean remove(Object elementObj) {
            UIComponent element = (UIComponent) elementObj;
            if (element == null) {
                throw new NullPointerException();
            }

            if (super.remove(element)) {
                element.setParent(null);
                return (true);
            } else {
                return (false);
            }
        }

        public boolean removeAll(Collection<?> collection) {
            boolean result = false;
            Iterator<?> elements = collection.iterator();
            while (elements.hasNext()) {
                if (remove(elements.next())) {
                    result = true;
                }
            }
            return (result);
        }

        public boolean retainAll(Collection<?> collection) {
            boolean modified = false;
            Iterator<?> items = iterator();
            while (items.hasNext()) {
                if (!collection.contains(items.next())) {
                    items.remove();
                    modified = true;
                }
            }
            return (modified);
        }

        public UIComponent set(int index, UIComponent element) {
            if (element == null) {
                throw new NullPointerException();
            } else if ((index < 0) || (index >= size())) {
                throw new IndexOutOfBoundsException();
            } else {
                eraseParent(element);
                UIComponent previous =
                    (UIComponent) get(index);
                previous.setParent(null);
                element.setParent(UIComponentBase.this);
                super.set(index, element);
                return (previous);
            }
        }

    }


    // Private implementation of ListIterator for ChildrenList
    private static class ChildrenListIterator implements ListIterator<UIComponent> {


        public ChildrenListIterator(ChildrenList list) {
            this.list = list;
            this.index = 0;
        }

        public ChildrenListIterator(ChildrenList list, int index) {
            this.list = list;
            if ((index < 0) || (index >= list.size())) {
                throw new IndexOutOfBoundsException("" + index);
            } else {
                this.index = index;
            }
        }


        private ChildrenList list;
        private int index;
        private int last = -1; // Index last returned by next() or previous()

        // Iterator methods

        public boolean hasNext() {
            return (index < list.size());
        }

        public UIComponent next() {
            try {
                UIComponent o = list.get(index);
                last = index++;
                return (o);
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException("" + index);
            }
        }

        public void remove() {
            if (last == -1) {
                throw new IllegalStateException();
            }
            list.remove(last);
            if (last < index) {
                index--;
            }
            last = -1;
        }

        // ListIterator methods

        public void add(UIComponent o) {
            last = -1;
            list.add(index++, o);
        }

        public boolean hasPrevious() {
            return (index > 1);
        }

        public int nextIndex() {
            return (index);
        }

        public UIComponent previous() {
            try {
                int current = index - 1;
                UIComponent o = list.get(current);
                last = current;
                index = current;
                return (o);
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public int previousIndex() {
            return (index - 1);
        }

        public void set(UIComponent o) {
            if (last == -1) {
                throw new IllegalStateException();
            }
            list.set(last, o);
        }

    }


    // Private implementation of Iterator for getFacetsAndChildren()
    private final static class FacetsAndChildrenIterator implements Iterator<UIComponent> {
        
        private Iterator<UIComponent> iterator;
        private boolean childMode;
        private UIComponent c;

        public FacetsAndChildrenIterator(UIComponent c) {
            this.c = c;
            this.childMode = false;
        }
        
        private void update() {
            if (this.iterator == null) {
                // we must guarantee that 'iterator' is never null
                if (this.c.getFacetCount() != 0) {
                    this.iterator = this.c.getFacets().values().iterator();
                    this.childMode = false;
                } else if (this.c.getChildCount() != 0) {
                    this.iterator = this.c.getChildren().iterator();
                    this.childMode = true;
                } else {
                    this.iterator = EMPTY_ITERATOR;
                    this.childMode = true;
                }
            } else if (!this.childMode
                    && !this.iterator.hasNext()
                    && this.c.getChildCount() != 0) {
                this.iterator = this.c.getChildren().iterator();
                this.childMode = true;
            }
        }

        public boolean hasNext() {
            this.update();
            return this.iterator.hasNext();
        }

        public UIComponent next() {
            this.update();
            return this.iterator.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }


    // Private implementation of Map that supports the functionality
    // required by UIComponent.getFacets()
    private class FacetsMap extends HashMap<String, UIComponent> {

        public void clear() {
            Iterator<String> keys = keySet().iterator();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                keys.remove();
            }
            super.clear();
        }

        public Set<Map.Entry<String, UIComponent>> entrySet() {
            return (new FacetsMapEntrySet(this));
        }

        public Set<String> keySet() {
            return (new FacetsMapKeySet(this));
        }

        public UIComponent put(String key, UIComponent value) {
            if ((key == null) || (value == null)) {
                throw new NullPointerException();
            } else if (!(key instanceof String) ||
                       !(value instanceof UIComponent)) {
                throw new ClassCastException();
            }
            UIComponent previous = super.get(key);
            if (previous != null) {
                previous.setParent(null);
            }
            eraseParent(value);
            value.setParent(UIComponentBase.this);
            return (super.put(key, value));
        }

        public void putAll(Map<? extends String, ? extends UIComponent> map) {
            if (map == null) {
                throw new NullPointerException();
            }            
            for (Map.Entry<? extends String, ? extends UIComponent> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        public UIComponent remove(Object key) {
            UIComponent previous = get(key);
            if (previous != null) {
                previous.setParent(null);
            }
            super.remove(key);
            return (previous);
        }

        public Collection<UIComponent> values() {
            return (new FacetsMapValues(this));
        }

        Iterator<String> keySetIterator() {
            return ((new ArrayList(super.keySet())).iterator());
        }

    }


    // Private implementation of Set for FacetsMap.getEntrySet()
    private static class FacetsMapEntrySet extends AbstractSet<Map.Entry<String, UIComponent>> {

        public FacetsMapEntrySet(FacetsMap map) {
            this.map = map;
        }

        private FacetsMap map = null;

        public boolean add(Map.Entry<String, UIComponent> o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends Map.Entry<String,UIComponent>> c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            map.clear();
        }

        public boolean contains(Object o) {
            if (o == null) {
                throw new NullPointerException();
            }
            if (!(o instanceof Map.Entry)) {
                return (false);
            }
            Map.Entry e = (Map.Entry) o;
            Object k = e.getKey();
            Object v = e.getValue();
            if (!map.containsKey(k)) {
                return (false);
            }
            if (v == null) {
                return (map.get(k) == null);
            } else {
                return (v.equals(map.get(k)));
            }
        }

        public boolean isEmpty() {
            return (map.isEmpty());
        }

        public Iterator<Map.Entry<String, UIComponent>> iterator() {
            return (new FacetsMapEntrySetIterator(map));
        }

        public boolean remove(Object o) {
            if (o == null) {
                throw new NullPointerException();
            }
            if (!(o instanceof Map.Entry)) {
                return (false);
            }
            Object k = ((Map.Entry) o).getKey();
            if (map.containsKey(k)) {
                map.remove(k);
                return (true);
            } else {
                return (false);
            }
        }

        public boolean removeAll(Collection c) {
            boolean result = false;
            Iterator v = c.iterator();
            while (v.hasNext()) {
                if (remove(v.next())) {
                    result = true;
                }
            }
            return (result);
        }

        public boolean retainAll(Collection c) {
            boolean result = false;
            Iterator v = iterator();
            while (v.hasNext()) {
                if (!c.contains(v.next())) {
                    v.remove();
                    result = true;
                }
            }
            return (result);
        }

        public int size() {
            return (map.size());
        }

    }


    // Private implementation of Map.Entry for FacetsMapEntrySet
    private static class FacetsMapEntrySetEntry implements Map.Entry<String, UIComponent> {

        public FacetsMapEntrySetEntry(FacetsMap map, String key) {
            this.map = map;
            this.key = key;
        }

        private FacetsMap map;
        private String key;

        public boolean equals(Object o) {
            if (o == null) {
                return (false);
            }
            if (!(o instanceof Map.Entry)) {
                return (false);
            }
            Map.Entry e = (Map.Entry) o;
            if (key == null) {
                if (e.getKey() != null) {
                    return (false);
                }
            } else {
                if (!key.equals(e.getKey())) {
                    return (false);
                }
            }
            UIComponent v = map.get(key);
            if (v == null) {
                if (e.getValue() != null) {
                    return (false);
                }
            } else {
                if (!v.equals(e.getValue())) {
                    return (false);
                }
            }
            return (true);
        }

        public String getKey() {
            return (key);
        }

        public UIComponent getValue() {
            return (map.get(key));
        }

        public int hashCode() {
            Object value = map.get(key);
            return (((key == null) ? 0 : key.hashCode()) ^
                    ((value == null) ? 0 : value.hashCode()));
        }

        public UIComponent setValue(UIComponent value) {
            UIComponent previous = map.get(key);
            map.put(key, value);
            return (previous);
        }

    }


    // Private implementation of Set for FacetsMap.getEntrySet().iterator()
    private static class FacetsMapEntrySetIterator implements Iterator<Map.Entry<String, UIComponent>> {

        public FacetsMapEntrySetIterator(FacetsMap map) {
            this.map = map;
            this.iterator = map.keySetIterator();
        }

        private FacetsMap map = null;
        private Iterator<String> iterator = null;
        private Map.Entry<String, UIComponent> last = null;

        public boolean hasNext() {
            return (iterator.hasNext());
        }

        public Map.Entry<String, UIComponent> next() {
            last = new FacetsMapEntrySetEntry(map, iterator.next());
            return (last);
        }

        public void remove() {
            if (last == null) {
                throw new IllegalStateException();
            }
            map.remove(((Map.Entry) last).getKey());
            last = null;
        }

    }


    // Private implementation of Set for FacetsMap.getKeySet()
    private static class FacetsMapKeySet extends AbstractSet<String> {

        public FacetsMapKeySet(FacetsMap map) {
            this.map = map;
        }

        private FacetsMap map = null;

        public boolean add(String o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends String> c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            map.clear();
        }

        public boolean contains(Object o) {
            return (map.containsKey(o));
        }

        public boolean containsAll(Collection c) {
            Iterator v = c.iterator();
            while (v.hasNext()) {
                if (!map.containsKey(v.next())) {
                    return (false);
                }
            }
            return (true);
        }

        public boolean isEmpty() {
            return (map.size() == 0);
        }

        public Iterator<String> iterator() {
            return (new FacetsMapKeySetIterator(map));
        }

        public boolean remove(Object o) {
            if (map.containsKey(o)) {
                map.remove(o);
                return (true);
            } else {
                return (false);
            }
        }

        public boolean removeAll(Collection c) {
            boolean result = false;
            Iterator v = c.iterator();
            while (v.hasNext()) {
                Object o = v.next();
                if (map.containsKey(o)) {
                    map.remove(o);
                    result = true;
                }
            }
            return (result);
        }

        public boolean retainAll(Collection c) {
            boolean result = false;
            Iterator v = iterator();
            while (v.hasNext()) {
                if (!c.contains(v.next())) {
                    v.remove();
                    result = true;
                }
            }
            return (result);
        }

        public int size() {
            return (map.size());
        }

    }


    // Private implementation of Set for FacetsMap.getKeySet().iterator()
    private static class FacetsMapKeySetIterator implements Iterator<String> {

        public FacetsMapKeySetIterator(FacetsMap map) {
            this.map = map;
            this.iterator = map.keySetIterator();
        }

        private FacetsMap map = null;
        private Iterator<String> iterator = null;
        private String last = null;

        public boolean hasNext() {
            return (iterator.hasNext());
        }

        public String next() {
            last = iterator.next();
            return (last);
        }

        public void remove() {
            if (last == null) {
                throw new IllegalStateException();
            }
            map.remove(last);
            last = null;
        }

    }


    // Private implementation of Collection for FacetsMap.values()
    private static class FacetsMapValues extends AbstractCollection<UIComponent> {

        public FacetsMapValues(FacetsMap map) {
            this.map = map;
        }

        private FacetsMap map;

        public boolean add(UIComponent o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            map.clear();
        }

        public boolean isEmpty() {
            return (map.size() == 0);
        }

        public Iterator<UIComponent> iterator() {
            return (new FacetsMapValuesIterator(map));
        }

        public int size() {
            return (map.size());
        }


    }


    // Private implementation of Iterator for FacetsMap.values().iterator()
    private static class FacetsMapValuesIterator implements Iterator<UIComponent> {

        public FacetsMapValuesIterator(FacetsMap map) {
            this.map = map;
            this.iterator = map.keySetIterator();
        }

        private FacetsMap map = null;
        private Iterator<String> iterator = null;
        private Object last = null;

        public boolean hasNext() {
            return (iterator.hasNext());
        }

        public UIComponent next() {
            last = iterator.next();
            return (map.get(last));
        }

        public void remove() {
            if (last == null) {
                throw new IllegalStateException();
            }
            map.remove(last);
            last = null;
        }

    }


   

}
