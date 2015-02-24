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
package eclipselink.extender;

import java.util.Hashtable;

import javax.persistence.spi.PersistenceProvider;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

/**
 * Bundle activator. Just registers our persistence provider with the service registry.
 */
public class Activator implements BundleActivator {
	@Override
	public void start(BundleContext context) throws Exception {
		TransactionController.initialize(context);
		OurPersistenceProvider provider = new OurPersistenceProvider();
		Hashtable<String, Object> dict = new Hashtable<>();
		dict.put( EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER, "org.eclipse.persistence.jpa.PersistenceProvider");
		context.registerService(PersistenceProvider.class, provider, dict);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		TransactionController.destroy();
	}
}