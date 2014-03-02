if FileTest.exists?("/tmp/payload/launch-params")

  configs = File.read("/tmp/payload/launch-params").split(",").map(&:strip)

  configs.each { |x| key_value_pair = x.split("=").map(&:strip)
    Facter.add("stratos_" + key_value_pair[0].to_s){
      setcode {
        key_value_pair[1].to_s
      }
    }
  }
end
