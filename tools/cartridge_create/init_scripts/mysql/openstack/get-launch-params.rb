#! /usr/bin/ruby

### get-launch-params.rb

# The following script obtains the launch parameters from 
# the file /tmp/payload/launch-params, then parses out the 
# parameters for this instance by using the launch index
# of this particular EC2 instance.
#
# Pass the command the -e flag to output the instance 
# parameters as exports of shell variables. Any other 
# arguments are ignored.

def get_launch_params(launch_params_file)
  IO.readlines launch_params_file
end

export_stmt = ""

launch_params = get_launch_params(
  "/var/lib/cloud/instance/payload/launch-params")

if launch_params.length > 0
  instance_params_str = launch_params[0]

  instance_params = instance_params_str.split(',')

  export_stmt = "export " if ARGV.length > 0 && ARGV.include?("-e")

  instance_params.each { |param|
    puts export_stmt + param
  }

end

