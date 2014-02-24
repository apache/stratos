class mysql{

  if $stratos_mysql_password {
    $root_password = $stratos_mysql_password
  }
  else {
    $root_password = 'root'
  }

  package { ['mysql-server','phpMyAdmin.noarch','httpd']:
    ensure => installed,
  }

  service { 'mysqld':
    ensure  => running,
    pattern => 'mysqld',
    require => Package['mysql-server'],
  }

  service { 'httpd':
    ensure  => running,
    pattern => 'httpd',
    require => Package['httpd'],
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

  file { '/etc/httpd/conf.d/phpMyAdmin.conf':
    ensure  => present,
    content => template('mysql/phpMyAdmin.conf.erb'),
    notify  => Service['httpd'],
    require => [
      Package['phpMyAdmin.noarch'],
      Package['httpd'],
    ];
  }
}
