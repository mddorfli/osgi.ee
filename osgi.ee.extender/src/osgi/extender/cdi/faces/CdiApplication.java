/*
 * Copyright 2015, Imtech Traffic & Infra
 * Copyright 2015, aVineas IT Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package osgi.extender.cdi.faces;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.el.CompositeELResolver;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.jboss.weld.el.WeldELContextListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Application wrapper. Wraps another application and performs some special actions on the faces
 * application to integrate with CDI, most notably: adding EL resolvers and wrapping an expression 
 * factory to take care of CDI integration.
 * 
 * @author Arie van Wijngaarden
 */
class CdiApplication extends ApplicationWrapper {
    private Application delegate;
    private ServiceTracker<BeanManager, BeanManager> beanManagerTracker;
    
    /**
     * Create a new wrapped application for this instance.
     * 
     * @param delegate The wrapped application object
     */
    CdiApplication(Application delegate) {
        this.delegate = delegate;
        delegate.addELContextListener(new WeldELContextListener());
    }
    
    /**
     * Get the bundle context. It is retrieved from the servlet attribute as per OSGi web specifiction.
     * 
     * @return The bundle context
     */
    private static BundleContext context() {
        FacesContext context = FacesContext.getCurrentInstance();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
        final BundleContext bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        return bundleContext;
    }

    /**
     * Get the filter specification as present in the init parameter.
     * 
     * @return The filter specification. May be null
     */
    private static String getFilter() {
        return FacesContext.getCurrentInstance().getExternalContext().getInitParameter("osgi.extender.cdi.faces.filter");
    }
    
    /**
     * Perform a check on the set-up. Since during construction of this application we don't know anything about
     * the bean managers and resource handlers (since the faces context may not even be created), we need to 
     * perform this checking at the specific overrides.
     */
    private synchronized void check() {
        if (beanManagerTracker == null) {
            final BundleContext bundleContext = context();
            if (bundleContext != null) {
                String filterSpec = getFilter();
                String filter = "(" + Constants.OBJECTCLASS + "=" + BeanManager.class.getName() + ")";
                if (filterSpec != null) {
                    filter = "(&" + filter + filterSpec + ")";
                }
                try {
                    beanManagerTracker = new ServiceTracker<>(bundleContext, 
                            FrameworkUtil.createFilter(filter), null);
                    beanManagerTracker.open();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Perform an action for the tracked bean managers.
     * 
     * @param consumer The consumer to execute for each manager tracked
     */
    private void doWithBeanManagers(Consumer<BeanManager> consumer) {
        List<BeanManager> managers = new ArrayList<>();
        check();
        if (beanManagerTracker != null) {
            managers.addAll(beanManagerTracker.getTracked().values());
        }
        managers.stream().forEach(consumer);
    }

    /**
     * Get the EL resolver. The EL resolver is an EL resolver consisting of
     * a compound of the resolvers of all found bean managers.
     */
    @Override
    public ELResolver getELResolver() {
        final CompositeELResolver resolver = new CompositeELResolver();
        doWithBeanManagers((b) -> resolver.add(b.getELResolver()));
        resolver.add(delegate.getELResolver());
        return resolver;
    }

    /**
     * Get the expression factory. This is the wrapped expression factory using
     * all bean managers found.
     */
    @Override
    public ExpressionFactory getExpressionFactory() {
        Wrapper wrapped = new Wrapper(delegate.getExpressionFactory());
        doWithBeanManagers((b) -> wrapped.factory = b.wrapExpressionFactory(wrapped.factory));
        return wrapped.factory;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        try {
            return new BundleResourceHandler(context(), delegate.getResourceHandler(), getFilter());
        } catch (Exception exc) {
            exc.printStackTrace();
            return delegate.getResourceHandler();
        }
    }

    @Override
    public Application getWrapped() {
        return delegate;
    }
    
    private static class Wrapper {
        ExpressionFactory factory;
        Wrapper(ExpressionFactory f) {
            this.factory = f;
        }
    }
}
