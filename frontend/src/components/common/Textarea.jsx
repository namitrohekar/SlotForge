import { controlStyles } from './controlStyles.js'

export default function Textarea({ className = '', rows = 3, ...props }) {
  return <textarea rows={rows} className={`px-3 py-2 ${controlStyles} ${className}`} {...props} />
}
