package org.apache.syncope.core.provisioning.camel.processor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class GroupCreateInPullProcessor implements Processor {

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Override
    @SuppressWarnings("unchecked")
    public void process(final Exchange exchange) {
        WorkflowResult<String> created = (WorkflowResult<String>) exchange.getIn().getBody();

        GroupTO groupTO = exchange.getProperty("any", GroupTO.class);
        Map<String, String> groupOwnerMap = exchange.getProperty("groupOwnerMap", Map.class);
        Set<String> excludedResources = exchange.getProperty("excludedResources", Set.class);
        Boolean nullPriorityAsync = exchange.getProperty("nullPriorityAsync", Boolean.class);

        AttrTO groupOwner = groupTO.getPlainAttrMap().get(StringUtils.EMPTY);
        if (groupOwner != null) {
            groupOwnerMap.put(created.getResult(), groupOwner.getValues().iterator().next());
        }

        List<PropagationTask> tasks = propagationManager.getCreateTasks(
                AnyTypeKind.GROUP,
                created.getResult(),
                created.getPropByRes(),
                groupTO.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        exchange.getOut().setBody(new ImmutablePair<>(created.getResult(), null));
    }
}
