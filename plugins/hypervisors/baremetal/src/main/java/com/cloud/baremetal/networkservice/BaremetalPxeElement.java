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
//
// Automatically generated by addcopyright.py at 01/29/2013
package com.cloud.baremetal.networkservice;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.cloud.baremetal.database.BaremetalPxeVO;
import com.cloud.baremetal.manager.BaremetalVlanManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.IllegalVirtualMachineException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class BaremetalPxeElement extends AdapterBase implements NetworkElement {
  private static final Logger s_logger = Logger.getLogger(BaremetalPxeElement.class);
  private static final Map<Service, Map<Capability, String>> capabilities;

  @Inject
  BaremetalPxeManager _pxeMgr;;
  @Inject
  VMInstanceDao _vmDao;
  @Inject
  NicDao _nicDao;
  @Inject
  BaremetalVlanManager vlanMgr;
  @Inject
  DataCenterDao zoneDao;

  static {
    final Capability cap = new Capability(BaremetalPxeManager.BAREMETAL_PXE_CAPABILITY);
    final Map<Capability, String> baremetalCaps = new HashMap<Capability, String>();
    baremetalCaps.put(cap, null);
    capabilities = new HashMap<Service, Map<Capability, String>>();
    capabilities.put(BaremetalPxeManager.BAREMETAL_PXE_SERVICE, baremetalCaps);
  }

  @Override
  public Map<Service, Map<Capability, String>> getCapabilities() {
    return capabilities;
  }

  @Override
  public Provider getProvider() {
    return BaremetalPxeManager.BAREMETAL_PXE_SERVICE_PROVIDER;
  }

  private boolean canHandle(DeployDestination dest, TrafficType trafficType, GuestType networkType) {
    final Pod pod = dest.getPod();
    if (pod != null && trafficType == TrafficType.Guest) {
      final QueryBuilder<BaremetalPxeVO> sc = QueryBuilder.create(BaremetalPxeVO.class);
      sc.and(sc.entity().getPodId(), Op.EQ, pod.getId());
      return sc.find() != null;
    }

    return false;
  }

  @Override
  public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
  ResourceUnavailableException, InsufficientCapacityException {
    if (dest.getDataCenter().getNetworkType() == DataCenter.NetworkType.Advanced){
      return true;
    }

    if (offering.isSystemOnly() || !canHandle(dest, offering.getTrafficType(), network.getGuestType())) {
      s_logger.debug("BaremetalPxeElement can not handle network offering: " + offering.getName());
      return false;
    }
    return true;
  }

  @Override
  @DB
  public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
      throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, IllegalVirtualMachineException {
    if (vm.getType() != Type.User || vm.getHypervisorType() != HypervisorType.BareMetal) {
      throw new IllegalVirtualMachineException("Illegal VM type informed. Excpeted USER VM, but got -> " + vm.getType());
    }

    final VMInstanceVO vo = _vmDao.findById(vm.getId());
    assert vo != null : "Where ths nic " + nic.getId() + " going???";
    if (vo.getLastHostId() == null) {
      nic.setMacAddress(dest.getHost().getPrivateMacAddress());
      final NicVO nicVo = _nicDao.findById(nic.getId());
      nicVo.setMacAddress(nic.getMacAddress());
      _nicDao.update(nicVo.getId(), nicVo);

      /*This vm is just being created */
      if (!_pxeMgr.prepare(vm, nic, network, dest, context)) {
        throw new CloudRuntimeException("Cannot prepare pxe server");
      }
    }

    if (dest.getDataCenter().getNetworkType() == DataCenter.NetworkType.Advanced){
      prepareVlan(network, dest);
    }

    return true;
  }

  private void prepareVlan(Network network, DeployDestination dest) {
    vlanMgr.prepareVlan(network, dest);
  }

  @Override
  public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException,
  ResourceUnavailableException {
    if (vm.getType() != Type.User || vm.getHypervisorType() != HypervisorType.BareMetal) {
      return false;
    }

    final DataCenterVO dc = zoneDao.findById(vm.getVirtualMachine().getDataCenterId());
    if (dc.getNetworkType() == DataCenter.NetworkType.Advanced) {
      releaseVlan(network, vm);
    }
    return true;
  }

  private void releaseVlan(Network network, VirtualMachineProfile vm) {
    vlanMgr.releaseVlan(network, vm);
  }

  @Override
  public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
    return true;
  }

  @Override
  public boolean isReady(PhysicalNetworkServiceProvider provider) {
    return true;
  }

  @Override
  public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException,
  ResourceUnavailableException {
    return true;
  }

  @Override
  public boolean canEnableIndividualServices() {
    return false;
  }

  @Override
  public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
    return true;
  }

  @Override
  public boolean verifyServicesCombination(Set<Service> services) {
    return true;
  }
}