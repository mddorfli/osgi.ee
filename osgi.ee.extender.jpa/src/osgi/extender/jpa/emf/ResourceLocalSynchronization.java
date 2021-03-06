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
package osgi.extender.jpa.emf;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.transaction.Status;

/**
 * Resource local synchronization.
 */
class ResourceLocalSynchronization extends BaseSynchronization {
    private EntityTransaction trans;

    ResourceLocalSynchronization(EntityTransaction t, EntityManager em, ThreadLocal<EntityManager> l) {
        super(em, l);
        trans = t;
    }

    @Override
    protected void doAfterCompletion(int status) {
        if (status == Status.STATUS_ROLLING_BACK || status == Status.STATUS_MARKED_ROLLBACK ||
            status == Status.STATUS_ROLLEDBACK) {
            trans.rollback();
        }
        else {
            trans.commit();
        }
    }
}
