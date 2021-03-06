// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.affinity;

import java.util.List;

import com.cloud.uservm.UserVm;

import org.apache.cloudstack.api.command.user.affinitygroup.CreateAffinityGroupCmd;

public interface AffinityGroupService {

    /**
     * Creates an affinity/anti-affinity group for the given account/domain.
     *
     * @param accountName
     * @param projectId
     * @param domainId
     * @param affinityGroupName
     * @param affinityGroupType
     * @param description
     * @return AffinityGroup
     */
    AffinityGroup createAffinityGroup(String accountName, Long projectId, Long domainId, String affinityGroupName, String affinityGroupType, String description);

    AffinityGroup createAffinityGroup(CreateAffinityGroupCmd createAffinityGroupCmd);

    /**
     * Creates an affinity/anti-affinity group.
     *
     * @param affinityGroupId
     * @param accountName
     * @param domainId
     * @param affinityGroupName
     */
    boolean deleteAffinityGroup(Long affinityGroupId, String accountName, Long projectId, Long domainId, String affinityGroupName);

    /**
     * List group types available in deployment
     *
     * @return
     */
    List<String> listAffinityGroupTypes();

    AffinityGroup getAffinityGroup(Long groupId);

    UserVm updateVMAffinityGroups(Long vmId, List<Long> affinityGroupIds);

    boolean isAffinityGroupProcessorAvailable(String affinityGroupType);

    boolean isAdminControlledGroup(AffinityGroup group);

    boolean isAffinityGroupAvailableInDomain(long affinityGroupId, long domainId);


}
