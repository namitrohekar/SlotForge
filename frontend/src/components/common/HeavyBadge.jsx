import Badge from './Badge.jsx'
import Tooltip from './Tooltip.jsx'

export default function HeavyBadge() {
  return (
    <Tooltip text="Demanding paper: a student gets rest days between these.">
      <span>
        <Badge tone="warning">Heavy</Badge>
      </span>
    </Tooltip>
  )
}
