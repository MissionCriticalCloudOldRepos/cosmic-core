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
package com.cloud.network.dao;

import java.util.List;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

import org.springframework.stereotype.Component;

@Component
@DB()
public class NetworkExternalFirewallDaoImpl extends GenericDaoBase<NetworkExternalFirewallVO, Long> implements NetworkExternalFirewallDao {

    final SearchBuilder<NetworkExternalFirewallVO> networkIdSearch;
    final SearchBuilder<NetworkExternalFirewallVO> deviceIdSearch;

    protected NetworkExternalFirewallDaoImpl() {
        super();
        networkIdSearch = createSearchBuilder();
        networkIdSearch.and("networkId", networkIdSearch.entity().getNetworkId(), Op.EQ);
        networkIdSearch.done();
        deviceIdSearch = createSearchBuilder();
        deviceIdSearch.and("externalFWDeviceId", deviceIdSearch.entity().getExternalFirewallDeviceId(), Op.EQ);
        deviceIdSearch.done();
    }

    @Override
    public NetworkExternalFirewallVO findByNetworkId(long networkId) {
        SearchCriteria<NetworkExternalFirewallVO> sc = networkIdSearch.create();
        sc.setParameters("networkId", networkId);
        return findOneBy(sc);
    }

    @Override
    public List<NetworkExternalFirewallVO> listByFirewallDeviceId(long fwDeviceId) {
        SearchCriteria<NetworkExternalFirewallVO> sc = deviceIdSearch.create();
        sc.setParameters("externalFWDeviceId", fwDeviceId);
        return search(sc, null);
    }
}
