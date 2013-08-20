;
$TTL 604800
$ORIGIN STRATOS_DOMAIN.
@       14400   IN      SOA     a.STRATOS_DOMAIN.    admin.STRATOS_DOMAIN. (
                                2012112614 ; Serial
                                28800 ; Refresh
                                3600 ; Retry
                                604800 ; Expire
                                38400 ) ; Negative Cache TTL
;
@       IN      A       ELB_IP
@       IN      NS      a.STRATOS_DOMAIN.
git     IN      NS      ELB_IP
adp     IN      NS      ELB_IP
notify.git        IN      NS      ADC_IP
