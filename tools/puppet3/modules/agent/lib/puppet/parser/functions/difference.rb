# difference.rb
module Puppet::Parser::Functions
  newfunction(:difference, :type => :rvalue
) do |arguments|

    # Two arguments are required
    raise(Puppet::ParseError, "difference(): Wrong number of arguments " +
      "given (#{arguments.size} for 2)") if arguments.size != 2

    first = arguments[0]
    second = arguments[1]

    unless first.is_a?(Array) && second.is_a?(Array)
      raise(Puppet::ParseError, 'difference(): Requires 2 arrays')
    end

    result = first - second

    return result
  end
end
