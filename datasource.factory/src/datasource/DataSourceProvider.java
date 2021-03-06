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
package datasource;

import org.apache.commons.dbcp.BasicDataSource;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;

/**
 * Provider of normal (pooled) data sources.
 */
@Component(immediate = true, property = {"service.pid=datasource"})
public class DataSourceProvider extends BasicDataSourceProvider<BasicDataSource> implements ManagedServiceFactory {

    @Override
    public String getName() {
        return "datasource";
    }

    @Override
    BasicDataSource getDataSource() {
        return new BasicDataSource();
    }
}
