import Button from './Button.jsx'
import Dialog from './Dialog.jsx'

export default function FilterSheet({ open, onClose, children }) {
  return (
    <Dialog open={open} onClose={onClose} title="Filters" footer={<Button onClick={onClose}>Show results</Button>}>
      <div className="flex flex-col gap-4">{children}</div>
    </Dialog>
  )
}
