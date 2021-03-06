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
package osgi.jta.servlet.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Transaction filter. Starts a transaction as part of a filter. Needs to be set as a filter
 * for a servlet to open and close transactions. If no transaction manager is found, the
 * filter just continues without transaction management.
 */
public class TransactionFilter implements Filter {
    private static final String ATTR = "$$Transaction$$Mutex";
    private ServiceTracker<TransactionManager, TransactionManager> tracker;
    
    private TransactionManager getManager(ServletContext sc) {
        if (tracker != null)
            return tracker.getService();
        BundleContext context = (BundleContext) sc.getAttribute("osgi-bundlecontext");
        if (context == null) {
            // Fall back in case we don't use a chapter 128 web extender.
            context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        }
        if (context == null) {
            // May occur if the bundle containing this class is not yet started
            // (but it soon will be).
            return null;
        }
        tracker = new ServiceTracker<>(context, TransactionManager.class, null);
        tracker.open();
        return getManager(sc);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws ServletException {
        TransactionManager manager = getManager(request.getServletContext());
        try {
            // Synchronize on the HTTP session. This to make sure that no race condition
            // occurs where the output is already flushed to the client before
            // the transaction is handled, possibly resulting in the client to
            // not see the last changes.
            Object toSynchronize = new Object();
            if (HttpServletRequest.class.isAssignableFrom(request.getClass())) {
                HttpServletRequest r = (HttpServletRequest) request;
                HttpSession session = r.getSession();
                toSynchronize = session.getAttribute(ATTR);
                if (toSynchronize == null) {
                    session.setAttribute(ATTR, toSynchronize = new Object());
                }
            }
            synchronized (toSynchronize) {
                if (manager != null)
                    manager.begin();
                chain.doFilter(request, response);
                if (manager != null) {
                    if (manager.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                        manager.rollback();
                    }
                    else {
                        manager.commit();
                    }
                }
            }
        } catch (Exception ex) {
            try {
                if (manager != null && manager.getStatus() != Status.STATUS_NO_TRANSACTION)
                    manager.rollback();
            } catch (Exception e) {
                e.printStackTrace();
            }
            throw new ServletException(ex);
        }
    }

    @Override
    public void init(FilterConfig config) {
        getManager(config.getServletContext());
    }
    
    @Override
    public void destroy() {
        if (tracker != null)
            tracker.close();
    }
}
