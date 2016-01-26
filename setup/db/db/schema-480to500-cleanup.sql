--;
-- Schema cleanup from 4.8.0 to 5.0.0;
--;

# Remove NetApp plugin
DROP TABLE IF EXISTS `cloud`.`netapp_volume` CASCADE CONSTRAINTS PURGE;
DROP TABLE IF EXISTS `cloud`.`netapp_pool` CASCADE CONSTRAINTS PURGE;
DROP TABLE IF EXISTS `cloud`.`netapp_lun` CASCADE CONSTRAINTS PURGE;

# Remove BigSwitch plugin
DROP TABLE IF EXISTS `cloud`.`external_bigswitch_bcf_devices` CASCADE CONSTRAINTS PURGE;
DROP TABLE IF EXISTS `cloud`.`external_bigswitch_vns_devices` CASCADE CONSTRAINTS PURGE;
