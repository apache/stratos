class mysql{

  if $stratos_mysql_password {
    $root_password = $stratos_mysql_password
  }
  else {
    $root_password = 'root'
  }

  package { ['mysql-server','phpmyadmin','apache2']:
    ensure => installed,
  }

  service { 'mysql':
    ensure  => running,
    pattern => 'mysql',
    require => Package['mysql-server'],
  }

  service { 'apache2':
    ensure  => running,
    pattern => 'apache2',
    require => Package['apache2'],
  }

#  exec { 'Set root password':
#    path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
#    unless  => "mysqladmin -uroot -p${root_password} status",
#    command => "mysqladmin -uroot password ${root_password}",
#    require => Service['mysqld'];
#  }
#
#  if $root_password {
#    exec {
#      'Delete anonymous users':
#        path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
#        command => "mysql -uroot -p${root_password} -Bse \"DELETE from mysql.user WHERE password=''\"",
#        require => Exec['Set root password'];
#
#      'Create mysql user root@%':
#        path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
#        command => "mysql -uroot -p${root_password} -Bse \"CREATE USER 'root'@'%' IDENTIFIED BY '${root_password}'\"",
#        require => Exec['Delete anonymous users'];
#    }
#  }

  file { '/etc/apache2/conf.d/phpmyadmin.conf':
    ensure  => present,
    content => template('mysql/phpMyAdmin.conf.erb'),
    notify  => Service['apache2'],
    require => [
      Package['phpmyadmin'],
      Package['apache2'],
    ];
  }
}
