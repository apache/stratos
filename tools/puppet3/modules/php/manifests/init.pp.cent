class php ($syslog="", $docroot="/var/www/html", $samlalias="") {
  $packages = [
    'httpd',
    'openssl.x86_64',
    'mod_ssl.x86_64',
    'php',
    'php-adodb.noarch',
    'php-dba.x86_64',
    'php-gd.x86_64',
    'php-imap.x86_64',
    'php-ldap.x86_64',
    'php-mcrypt.x86_64',
    'php-mysql.x86_64',
    'php-pear.noarch',
    'php-xml.x86_64',
    'php-xmlrpc.x86_64',
    'php.x86_64',
    'git-all.noarch',
    ]

#  file { '/etc/apt/apt.conf.d/90forceyes':
#    ensure => present,
#    source => 'puppet:///modules/php/90forceyes';
#  }

#  exec { 'update-apt':
#    path    => ['/bin', '/usr/bin'],
#    command => 'apt-get update > /dev/null 2>&1 &',
#    require => File['/etc/apt/apt.conf.d/90forceyes'],
#  }

  package { $packages:
    ensure   => installed,
  }

  # Apache
  file {
    '/etc/httpd/conf/httpd.conf':
      owner   => 'root',
      group   => 'root',
      mode    => '0775',
      notify  => Service['httpd'],
      content => template('php/httpd/httpd.conf.erb'),
      require => Package['httpd'];
#
#    '/etc/apache2/sites-available/default':
#      owner   => 'root',
#      group   => 'root',
#      mode    => '0775',
#      notify  => Service['apache2'],
#      content => template('php/apache2/sites-available/default.erb'),
#      require => Package['apache2'];
#
#    '/etc/apache2/sites-available/default-ssl':
#      owner   => 'root',
#      group   => 'root',
#      mode    => '0775',
#      notify  => Service['apache2'],
#      content => template('php/apache2/sites-available/default-ssl.erb'),
#      require => Package['apache2'];
  }

#  exec {
#    'enable ssl module':
#      path    => ['/bin', '/usr/bin', '/usr/sbin/'],
#      command => 'a2enmod ssl',
#      require => Package['apache2'];
#  }

  service { 'httpd':
    ensure    => running,
    name      => 'httpd',
    hasstatus => true,
    pattern   => 'httpd',
    require   => Package['httpd'];
  }

  exec { 'remove www contents':
    path    => '/bin/',
    command => "rm -rf /var/www/html/*",
    require => Package['httpd'],
  }

  # Apache end
#  exec { 'clone git repo': 
#    path     => ['/bin', '/usr/bin', '/usr/sbin/'],
#    cwd      => '/var/www',
#    command  => "git clone ${stratos_git_repo}",
#    require  => [
#      Package['git-all.noarch'],
#      Package['httpd'],
#    ]
#  }
}
