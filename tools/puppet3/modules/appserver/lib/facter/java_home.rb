Facter.add(:java_home) do
  setcode do
    java_home = ENV['JAVA_HOME']
  end
end
