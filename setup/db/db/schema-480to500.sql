--;
-- Schema upgrade from 4.8.0 to 5.0.0;
--;
ALTER TABLE `s2s_customer_gateway` MODIFY `guest_cidr_list` VARCHAR(4096);
