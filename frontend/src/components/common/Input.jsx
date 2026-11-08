import { controlStyles } from './controlStyles.js'

export default function Input({ className = '', ...props }) {
  return <input className={`h-control px-3 ${controlStyles} ${className}`} {...props} />
}
