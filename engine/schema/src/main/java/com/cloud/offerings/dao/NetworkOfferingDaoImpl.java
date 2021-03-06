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
package com.cloud.offerings.dao;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;

import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offering.NetworkOffering.Detail;
import com.cloud.offerings.NetworkOfferingDetailsVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

import org.springframework.stereotype.Component;

@Component
@DB()
public class NetworkOfferingDaoImpl extends GenericDaoBase<NetworkOfferingVO, Long> implements NetworkOfferingDao {
    final SearchBuilder<NetworkOfferingVO> NameSearch;
    final SearchBuilder<NetworkOfferingVO> SystemOfferingSearch;
    final SearchBuilder<NetworkOfferingVO> AvailabilitySearch;
    final SearchBuilder<NetworkOfferingVO> AllFieldsSearch;
    private final GenericSearchBuilder<NetworkOfferingVO, Long> UpgradeSearch;
    @Inject
    NetworkOfferingDetailsDao _detailsDao;

    protected NetworkOfferingDaoImpl() {
        super();

        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.and("uniqueName", NameSearch.entity().getUniqueName(), SearchCriteria.Op.EQ);
        NameSearch.done();

        SystemOfferingSearch = createSearchBuilder();
        SystemOfferingSearch.and("system", SystemOfferingSearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        SystemOfferingSearch.done();

        AvailabilitySearch = createSearchBuilder();
        AvailabilitySearch.and("availability", AvailabilitySearch.entity().getAvailability(), SearchCriteria.Op.EQ);
        AvailabilitySearch.and("isSystem", AvailabilitySearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        AvailabilitySearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("trafficType", AllFieldsSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("guestType", AllFieldsSearch.entity().getGuestType(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("isSystem", AllFieldsSearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        UpgradeSearch = createSearchBuilder(Long.class);
        UpgradeSearch.selectFields(UpgradeSearch.entity().getId());
        UpgradeSearch.and("physicalNetworkId", UpgradeSearch.entity().getId(), Op.NEQ);
        UpgradeSearch.and("physicalNetworkId", UpgradeSearch.entity().isSystemOnly(), Op.EQ);
        UpgradeSearch.and("trafficType", UpgradeSearch.entity().getTrafficType(), Op.EQ);
        UpgradeSearch.and("guestType", UpgradeSearch.entity().getGuestType(), Op.EQ);
        UpgradeSearch.and("state", UpgradeSearch.entity().getState(), Op.EQ);
        UpgradeSearch.done();
    }

    @Override
    public NetworkOfferingVO findByUniqueName(String uniqueName) {
        SearchCriteria<NetworkOfferingVO> sc = NameSearch.create();

        sc.setParameters("uniqueName", uniqueName);

        return findOneBy(sc);

    }

    @Override
    public NetworkOfferingVO persistDefaultNetworkOffering(NetworkOfferingVO offering) {
        assert offering.getUniqueName() != null : "how are you going to find this later if you don't set it?";
        NetworkOfferingVO vo = findByUniqueName(offering.getUniqueName());
        if (vo != null) {
            return vo;
        }
        try {
            vo = persist(offering);
            return vo;
        } catch (EntityExistsException e) {
            // Assume it's conflict on unique name from two different management servers.
            return findByUniqueName(offering.getName());
        }
    }

    @Override
    public List<NetworkOfferingVO> listSystemNetworkOfferings() {
        SearchCriteria<NetworkOfferingVO> sc = SystemOfferingSearch.create();
        sc.setParameters("system", true);
        return this.listIncludingRemovedBy(sc, null);
    }

    @Override
    public List<NetworkOfferingVO> listByAvailability(Availability availability, boolean isSystem) {
        SearchCriteria<NetworkOfferingVO> sc = AvailabilitySearch.create();
        sc.setParameters("availability", availability);
        sc.setParameters("isSystem", isSystem);
        return listBy(sc, null);
    }

    @Override
    @DB
    public boolean remove(Long networkOfferingId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        NetworkOfferingVO offering = findById(networkOfferingId);
        offering.setUniqueName(null);
        update(networkOfferingId, offering);
        boolean result = super.remove(networkOfferingId);
        txn.commit();
        return result;
    }

    @Override
    public List<Long> getOfferingIdsToUpgradeFrom(NetworkOffering originalOffering) {
        SearchCriteria<Long> sc = UpgradeSearch.create();
        // exclude original offering
        sc.addAnd("id", SearchCriteria.Op.NEQ, originalOffering.getId());

        // list only non-system offerings
        sc.addAnd("systemOnly", SearchCriteria.Op.EQ, false);

        // Type of the network should be the same
        sc.addAnd("guestType", SearchCriteria.Op.EQ, originalOffering.getGuestType());

        // Traffic types should be the same
        sc.addAnd("trafficType", SearchCriteria.Op.EQ, originalOffering.getTrafficType());

        sc.addAnd("state", SearchCriteria.Op.EQ, NetworkOffering.State.Enabled);

        //specify Vlan should be the same
        sc.addAnd("specifyVlan", SearchCriteria.Op.EQ, originalOffering.getSpecifyVlan());

        return customSearch(sc, null);
    }

    @Override
    public List<NetworkOfferingVO> listByTrafficTypeGuestTypeAndState(NetworkOffering.State state, TrafficType trafficType, Network.GuestType type) {
        SearchCriteria<NetworkOfferingVO> sc = AllFieldsSearch.create();
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("guestType", type);
        sc.setParameters("state", state);
        return listBy(sc, null);
    }

    @Override
    @DB
    public NetworkOfferingVO persist(NetworkOfferingVO off, Map<Detail, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        //1) persist the offering
        NetworkOfferingVO vo = super.persist(off);

        //2) persist the details
        if (details != null && !details.isEmpty()) {
            for (NetworkOffering.Detail detail : details.keySet()) {
                _detailsDao.persist(new NetworkOfferingDetailsVO(off.getId(), detail, details.get(detail)));
            }
        }

        txn.commit();
        return vo;
    }

}
