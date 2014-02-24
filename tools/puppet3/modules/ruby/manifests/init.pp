class ruby( $target = '/mnt' ) {

  if $stratos_app_path {
    $ruby_home = $stratos_app_path
  } 
  else {
    $ruby_home = "${target}/ruby"
  }

  file { '/mnt/ruby-start.sh':
    ensure  => present,
    mode    => '0755',
    content => template('ruby/ruby-start.sh.erb'),
    require => Exec['Create ruby home'];
  }

  exec {
    'Create ruby home':
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      command => "mkdir -p ${ruby_home}";

#    'Create ruby application':
#      path    => '/usr/local/rvm/gems/ruby-2.1.0/bin:/usr/local/rvm/gems/ruby-2.1.0@global/bin:/usr/local/rvm/rubies/ruby-2.1.0/bin',
#      cwd     => $ruby_home,
#      command => "rails new app",
#      require => Exec['Create ruby home'];
    
#    'Clone repo':
#      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
#      command => "git clone ${stratos_git_repo} ${ruby_home}/app",
#      require => Exec['Create ruby home'];

#    'Run script':
#      path    => '/usr/local/rvm/gems/ruby-2.1.0/bin:/usr/local/rvm/gems/ruby-2.1.0@global/bin:/usr/local/rvm/rubies/ruby-2.1.0/bin',
#      cwd     => "${ruby_home}/app",
#      command => "rails server > /dev/null 2>&1 &",
#      require => Exec['Create ruby application'];

    'Run start script':
      command => "/bin/bash /mnt/ruby-start.sh",
      require => File['/mnt/ruby-start.sh'];
  }
}
